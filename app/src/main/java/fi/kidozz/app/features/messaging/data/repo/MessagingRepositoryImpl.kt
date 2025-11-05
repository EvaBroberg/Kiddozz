package fi.kidozz.app.features.messaging.data.repo

import fi.kidozz.app.features.messaging.data.api.MessagingApiService
import fi.kidozz.app.features.messaging.data.api.SendMessageRequest
import fi.kidozz.app.features.messaging.data.db.MessagingDao
import fi.kidozz.app.features.messaging.data.db.MessageEntity
import fi.kidozz.app.features.messaging.data.db.ConversationEntity
import fi.kidozz.app.features.messaging.data.ws.MessagingWebSocketClient
import fi.kidozz.app.features.messaging.domain.model.*
import fi.kidozz.app.features.messaging.domain.repo.MessagingRepository
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.data.models.Educator
import fi.kidozz.app.data.models.Parent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class MessagingRepositoryImpl(
    private val apiService: MessagingApiService,
    private val dao: MessagingDao,
    private val webSocketClient: MessagingWebSocketClient,
    val kidsCache: kotlinx.coroutines.flow.StateFlow<List<Kid>>, // Made internal for logging
    val educatorsCache: kotlinx.coroutines.flow.StateFlow<List<Educator>> // Made internal for logging
) : MessagingRepository {

    override fun observeInbox(filter: ConversationType?): Flow<List<Conversation>> {
        return dao.observeConversationsByType(filter?.name?.lowercase())
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    override fun observeConversation(conversationId: String): Flow<List<Message>> {
        return dao.observeMessages(conversationId)
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    override suspend fun sendMessage(conversationId: String, text: String?, imageBytes: ByteArray?): Message {
        val messageId = UUID.randomUUID().toString()
        val message = Message(
            id = messageId,
            conversationId = conversationId,
            senderId = "current_user", // TODO: Get from auth
            body = text,
            imageUrl = null, // TODO: Upload image and get URL
            createdAt = System.currentTimeMillis(),
            isMine = true,
            status = DeliveryStatus.PENDING
        )

        // Insert as pending first
        dao.insertMessage(message.toEntity())

        try {
            // Send to API
            val response = apiService.sendMessage(conversationId, SendMessageRequest(text, null))
            if (response.isSuccessful) {
                val sentMessage = response.body()?.toDomain() ?: message
                dao.insertMessage(sentMessage.toEntity())
                return sentMessage
            } else {
                throw Exception("Failed to send message")
            }
        } catch (e: Exception) {
            // Keep as pending, will retry later
            throw e
        }
    }

    override suspend fun markAsRead(conversationId: String) {
        dao.markConversationAsRead(conversationId)
        try {
            apiService.markAsRead(conversationId)
        } catch (e: Exception) {
            // Log error but don't fail
        }
    }

    override suspend fun syncInitial() {
        try {
            val response = apiService.getConversations()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
                // Convert and insert conversations
                // This is a simplified implementation
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    override fun observeContactsInMyGroups(
        type: ContactType,
        myGroupIds: Set<String>,
        myUserId: String
    ): Flow<List<Contact>> = combine(kidsCache, educatorsCache) { kids, educators ->
        android.util.Log.d("MessagingRepo", "observeContactsInMyGroups: type=$type, myGroupIds=$myGroupIds (size=${myGroupIds.size}), myUserId=$myUserId")
        android.util.Log.d("MessagingRepo", "Source sizes: kidsCache.size=${kids.size}, educatorsCache.size=${educators.size}")
        
        // If no group IDs, return empty (don't show all contacts)
        if (myGroupIds.isEmpty()) {
            android.util.Log.w("MessagingRepo", "myGroupIds is empty! Returning empty contacts list.")
            return@combine emptyList<Contact>()
        }
        
        when (type) {
            ContactType.PARENT -> {
                // Kids who are in any of my groups
                val kidsInMyGroups = kids.filter { kid -> 
                    val kidGroupId = kid.group_id.toString() // group_id is String, no need for ?.
                    val matches = kidGroupId in myGroupIds
                    android.util.Log.d("MessagingRepo", "Kid ${kid.full_name}: group_id=$kidGroupId, in myGroupIds=$matches")
                    matches
                }
                android.util.Log.d("MessagingRepo", "Kids in my groups: ${kidsInMyGroups.size}")

                // All parents of those kids, excluding myself
                val extractedParents = kidsInMyGroups.flatMap { k -> k.parents }
                val filteredParents = extractedParents.filter { p -> p.id != myUserId }
                val distinctParents = filteredParents.distinctBy { it.id }
                
                android.util.Log.d("MessagingRepo", "Parents extraction: kidsInMyGroups=${kidsInMyGroups.size}, extractedParents=${extractedParents.size}, after self-exclusion=${filteredParents.size}, distinct=${distinctParents.size}")
                android.util.Log.d("MessagingRepo", "Final parent IDs: ${distinctParents.map { "${it.full_name}(${it.id})" }}")
                
                distinctParents
                    .map { p ->
                        Contact(
                            id = p.id,
                            name = p.full_name,
                            avatarUrl = null,
                            type = ContactType.PARENT
                        )
                    }
                    .sortedBy { it.name.lowercase() }
            }

            ContactType.EDUCATOR -> {
                // Educators that are assigned to any of my groups
                val filteredEducators = educators
                    .filter { e -> 
                        val matches = e.groups.any { g -> 
                            val groupIdString = g.id.toString()
                            val match = groupIdString in myGroupIds
                            android.util.Log.d("MessagingRepo", "Educator ${e.full_name}: group.id=$groupIdString, in myGroupIds=$match")
                            match
                        }
                        matches
                    }
                    .filter { e -> e.id != myUserId }
                
                android.util.Log.d("MessagingRepo", "Filtered educators: ${filteredEducators.size}")
                android.util.Log.d("MessagingRepo", "Final educator IDs: ${filteredEducators.map { "${it.full_name}(${it.id})" }}")
                
                filteredEducators
                    .distinctBy { it.id }
                    .map { e ->
                        Contact(
                            id = e.id,
                            name = e.full_name,
                            avatarUrl = null,
                            type = ContactType.EDUCATOR
                        )
                    }
                    .sortedBy { it.name.lowercase() }
            }
        }
    }.distinctUntilChanged()

    override fun observeAllEducatorsExcept(myUserId: String): Flow<List<Contact>> {
        return educatorsCache.map { educators ->
            android.util.Log.d("MessagingRepo", "observeAllEducatorsExcept: myUserId=$myUserId, educatorsCache.size=${educators.size}")
            android.util.Log.d("MessagingRepo", "All educator IDs in cache: ${educators.map { "${it.full_name}(${it.id})" }}")
            
            val filteredEducators = educators
                .filter { e -> e.id != myUserId } // Exclude self
            
            android.util.Log.d("MessagingRepo", "Filtered educators (excluding self): ${filteredEducators.size}")
            android.util.Log.d("MessagingRepo", "Final educator IDs: ${filteredEducators.map { "${it.full_name}(${it.id})" }}")
            
            filteredEducators
                .distinctBy { it.id }
                .map { e ->
                    Contact(
                        id = e.id,
                        name = e.full_name,
                        avatarUrl = null,
                        type = ContactType.EDUCATOR
                    )
                }
                .sortedBy { it.name.lowercase() }
        }.distinctUntilChanged()
    }

    override suspend fun createOrGetDirectConversation(contactId: String): String {
        // Try to find existing 1:1 conversation with that participant
        val existing = dao.findDirectConversationWith(contactId)
        if (existing != null) return existing.id

        // Create new conversation
        val conversationId = UUID.randomUUID().toString()
        
        // Upsert to Room
        dao.insertConversation(
            ConversationEntity(
                id = conversationId,
                title = resolveContactName(contactId),
                lastMessagePreview = null,
                lastTimestamp = System.currentTimeMillis(),
                unreadCount = 0,
                type = "direct", // Direct conversation
                participantsJson = buildParticipantsJson(contactId)
            )
        )
        return conversationId
    }

    private fun resolveContactName(contactId: String): String {
        // Try to find the contact name from caches
        val educator = educatorsCache.value.find { it.id == contactId }
        if (educator != null) return educator.full_name
        
        val parent = kidsCache.value
            .flatMap { it.parents }
            .find { it.id == contactId }
        if (parent != null) return parent.full_name
        
        return "Contact" // Fallback
    }

    private fun buildParticipantsJson(contactId: String): String {
        val participants = JSONArray()
        
        // Add current user (we'll need to get this from session)
        val currentUser = JSONObject().apply {
            put("id", "current_user") // TODO: Get from session
            put("name", "Me")
            put("role", "current")
        }
        participants.put(currentUser)
        
        // Add contact
        val contact = JSONObject().apply {
            put("id", contactId)
            put("name", resolveContactName(contactId))
            put("role", "contact")
        }
        participants.put(contact)
        
        return participants.toString()
    }

    // Extension functions for mapping between domain and data models
    private fun ConversationEntity.toDomain(): Conversation {
        val participants = try {
            val jsonArray = JSONArray(participantsJson)
            (0 until jsonArray.length()).map { i ->
                val participant = jsonArray.getJSONObject(i)
                Participant(
                    id = participant.getString("id"),
                    name = participant.getString("name"),
                    avatarUrl = participant.optString("avatarUrl").takeIf { it.isNotEmpty() },
                    role = when (participant.getString("role").lowercase()) {
                        "parent" -> ConversationType.PARENT
                        "educator" -> ConversationType.EDUCATOR
                        else -> ConversationType.GROUP
                    }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

        return Conversation(
            id = id,
            title = title,
            lastMessagePreview = lastMessagePreview,
            lastTimestamp = lastTimestamp,
            unreadCount = unreadCount,
            type = when (type.lowercase()) {
                "parent" -> ConversationType.PARENT
                "educator" -> ConversationType.EDUCATOR
                "group" -> ConversationType.GROUP
                else -> ConversationType.PARENT
            },
            participants = participants
        )
    }

    private fun MessageEntity.toDomain(): Message {
        return Message(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            body = body,
            imageUrl = imageUrl,
            createdAt = createdAt,
            isMine = isMine,
            status = when (status.lowercase()) {
                "pending" -> DeliveryStatus.PENDING
                "sent" -> DeliveryStatus.SENT
                "delivered" -> DeliveryStatus.DELIVERED
                "read" -> DeliveryStatus.READ
                else -> DeliveryStatus.SENT
            }
        )
    }

    private fun Message.toEntity(): MessageEntity {
        return MessageEntity(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            body = body,
            imageUrl = imageUrl,
            createdAt = createdAt,
            isMine = isMine,
            status = status.name.lowercase()
        )
    }

    // Extension function for MessageDto
    private fun fi.kidozz.app.features.messaging.data.api.MessageDto.toDomain(): Message {
        return Message(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            body = body,
            imageUrl = imageUrl,
            createdAt = createdAt,
            isMine = senderId == "current_user", // TODO: Get from auth
            status = when (status.lowercase()) {
                "pending" -> DeliveryStatus.PENDING
                "sent" -> DeliveryStatus.SENT
                "delivered" -> DeliveryStatus.DELIVERED
                "read" -> DeliveryStatus.READ
                else -> DeliveryStatus.SENT
            }
        )
    }
}
