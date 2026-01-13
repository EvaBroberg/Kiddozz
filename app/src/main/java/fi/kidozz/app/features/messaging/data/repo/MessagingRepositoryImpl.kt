package fi.kidozz.app.features.messaging.data.repo

import fi.kidozz.app.features.messaging.data.api.MessagingApiService
import fi.kidozz.app.features.messaging.data.api.SendMessageRequest
import fi.kidozz.app.features.messaging.data.api.CreateDirectConversationRequest
import fi.kidozz.app.features.messaging.data.db.MessagingDao
import fi.kidozz.app.features.messaging.data.db.MessageEntity
import fi.kidozz.app.features.messaging.data.db.ConversationEntity
import fi.kidozz.app.features.messaging.data.ws.MessagingWebSocketClient
import fi.kidozz.app.features.messaging.data.ws.MessagingSseClient
import fi.kidozz.app.features.messaging.domain.model.*
import fi.kidozz.app.features.messaging.domain.repo.MessagingRepository
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.data.models.Educator
import fi.kidozz.app.data.models.Parent
import fi.kidozz.app.core.config.FeatureFlags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import com.google.gson.Gson
import fi.kidozz.app.features.messaging.data.api.MessagingEventDto
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class MessagingRepositoryImpl(
    private val apiService: MessagingApiService,
    private val dao: MessagingDao,
    private val webSocketClient: MessagingWebSocketClient, // Kept for backward compatibility, not used for new logic
    private val sseClient: MessagingSseClient,
    private val tokenManager: fi.kidozz.app.data.auth.TokenManager,
    val kidsCache: kotlinx.coroutines.flow.StateFlow<List<Kid>>, // Made internal for logging
    val educatorsCache: kotlinx.coroutines.flow.StateFlow<List<Educator>> // Made internal for logging
) : MessagingRepository {

    override fun observeInbox(filter: ConversationType?): Flow<List<Conversation>> {
        // For PARENT and EDUCATOR filters, we want to include direct conversations
        // Direct conversations are person-to-person and shouldn't be filtered by "parent" or "educator" string type
        // Pass null to fetch all conversations (including "direct" and "group")
        val daoFilter = when (filter) {
            ConversationType.PARENT, ConversationType.EDUCATOR -> null  // Include directs
            ConversationType.GROUP -> "group"
            null -> null  // Show all
        }
        
        return dao.observeConversationsByType(daoFilter)
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    override fun observeConversation(conversationId: String): Flow<List<Message>> {
        // TODO(LOG-REMOVE)
        android.util.Log.d("Dao", "observeMessages(conversationId=$conversationId)")
        return dao.observeMessages(conversationId)
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    override suspend fun sendMessage(conversationId: String, text: String?, imageBytes: ByteArray?): Message {
        // Generate client-side message ID
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
        android.util.Log.d("MessagingRepo", "sendMessage: created local pending message $messageId")

        // Feature flag guard: skip API call if messaging is disabled
        if (!FeatureFlags.MESSAGING_ANDROID) {
            android.util.Log.d("MessagingRepo", "Messaging disabled via feature flag; returning local message.")
            // Keep as PENDING for demo mode, or mark as SENT if desired
            return message
        }

        try {
            // Send to API with client_message_id
            val request = SendMessageRequest(
                body = text,
                image_url = null,
                client_message_id = messageId
            )
            val response = apiService.sendMessage(conversationId, request)
            
            if (response.isSuccessful) {
                // Update existing message status to SENT (don't insert duplicate)
                dao.updateMessageStatus(messageId, DeliveryStatus.SENT.name.lowercase())
                android.util.Log.d("MessagingRepo", "sendMessage: marked message $messageId as SENT")
                
                // Return message with SENT status
                return message.copy(status = DeliveryStatus.SENT)
            } else {
                // API call failed, keep message as PENDING
                android.util.Log.e("MessagingRepo", "sendMessage: failed for $messageId with HTTP code ${response.code()}")
                return message
            }
        } catch (e: Exception) {
            android.util.Log.e("MessagingRepo", "sendMessage: failed for $messageId with exception", e)
            // Keep as PENDING on error, will retry later
            return message
        }
    }

    override suspend fun markAsRead(conversationId: String) {
        dao.markConversationAsRead(conversationId)
        // Feature flag guard: skip API call if messaging is disabled
        if (FeatureFlags.MESSAGING_ANDROID) {
            try {
                apiService.markAsRead(conversationId)
            } catch (e: Exception) {
                // Log error but don't fail
                android.util.Log.e("MessagingRepo", "Failed to mark as read", e)
            }
        }
    }

    override suspend fun syncInitial() {
        // Feature flag guard: skip API call if messaging is disabled
        if (!FeatureFlags.MESSAGING_ANDROID) {
            android.util.Log.d("MessagingRepo", "Messaging disabled via feature flag; skipping sync.")
            return
        }

        try {
            // Sync conversations
            val conversationsResponse = apiService.getConversations(cursor = null, limit = 50)
            if (conversationsResponse.isSuccessful) {
                val conversations = conversationsResponse.body() ?: emptyList()
                android.util.Log.d("MessagingRepo", "Fetched ${conversations.size} conversations from server")
                
                // Convert and insert conversations
                for (conversationDto in conversations) {
                    try {
                        val entity = conversationDto.toEntity()
                        dao.insertConversation(entity)
                        
                        // Also sync messages for this conversation
                        val messagesResponse = apiService.getMessages(conversationDto.id, cursor = null, limit = 50)
                        if (messagesResponse.isSuccessful) {
                            val messages = messagesResponse.body() ?: emptyList()
                            if (messages.isNotEmpty()) {
                                val currentUserId = tokenManager.getUserId() ?: "unknown"
                                val messageEntities = messages.map { it.toEntity(currentUserId) }
                                dao.insertMessages(messageEntities)
                                android.util.Log.d("MessagingRepo", "Synced ${messages.size} messages for conversation ${conversationDto.id}")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MessagingRepo", "Error syncing conversation ${conversationDto.id}", e)
                    }
                }
                android.util.Log.d("MessagingRepo", "Successfully synced ${conversations.size} conversations")
            } else {
                android.util.Log.w("MessagingRepo", "Failed to fetch conversations: ${conversationsResponse.code()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("MessagingRepo", "Error during sync", e)
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
                    val kidGroupId = kid.group_id // group_id is already String
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
                            val groupIdString = g.id // id is already String
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
        // TODO(LOG-REMOVE)
        android.util.Log.d("RepoConv", "createOrGetDirectConversation contactId=$contactId")
        if (contactId.isBlank()) {
            // TODO(LOG-REMOVE)
            android.util.Log.e("RepoConv", "blank contactId")
            throw IllegalArgumentException("contactId blank")
        }
        
        // Try to find existing 1:1 conversation with that participant
        // TODO(LOG-REMOVE)
        android.util.Log.d("Dao", "findDirectConversationWith(contactId=$contactId)")
        val existing = dao.findDirectConversationWith(contactId)
        // TODO(LOG-REMOVE)
        android.util.Log.d("RepoConv", "existingId=${existing?.id ?: "null"}")
        if (existing != null) {
            require(existing.id.isNotBlank()) { "Existing conversation has blank ID" }
            // TODO(LOG-REMOVE)
            android.util.Log.d("RepoConv", "RETURN conversationId=${existing.id} blank=${existing.id.isBlank()}")
            return existing.id
        }

        // Determine user type from contactId by checking caches
        val userType = when {
            educatorsCache.value.any { it.id == contactId } -> "educator"
            kidsCache.value.flatMap { it.parents }.any { it.id == contactId } -> "parent"
            else -> throw IllegalStateException("Cannot determine user type for contactId: $contactId")
        }

        // Create new conversation via backend API
        val request = CreateDirectConversationRequest(
            with_user_type = userType,
            with_user_id = contactId
        )
        
        val response = apiService.createDirectConversation(request)
        
        if (response.isSuccessful && response.body() != null) {
            val responseBody = response.body()!!
            val conversationId = responseBody.conversation_id
            
            // Build ConversationEntity using backend-generated ID
            val conversationEntity = ConversationEntity(
                id = conversationId,
                title = resolveContactName(contactId),
                lastMessagePreview = null,
                lastTimestamp = System.currentTimeMillis(),
                unreadCount = 0,
                type = "direct", // Direct conversation
                participantsJson = buildParticipantsJson(contactId)
            )
            
            // Insert into local database
            try {
                dao.insertConversation(conversationEntity)
                // TODO(LOG-REMOVE)
                android.util.Log.d("RepoConv", "inserted conversationId=$conversationId from backend")
            } catch (e: Exception) {
                android.util.Log.e("MessagingRepo", "Failed to insert conversation", e)
                throw e
            }
            
            // TODO(LOG-REMOVE)
            android.util.Log.d("RepoConv", "RETURN conversationId=$conversationId blank=${conversationId.isBlank()}")
            return conversationId
        } else {
            // Log error body for debugging backend errors
            val errorBody = try {
                response.errorBody()?.string()
            } catch (e: Exception) {
                "Failed to read error body: ${e.message}"
            }
            android.util.Log.e("MessagingRepo", "createDirectConversation error body=${errorBody}")
            
            val errorMsg = if (response.code() > 0) {
                "Failed to create conversation: HTTP ${response.code()}"
            } else {
                "Failed to create conversation: ${response.message() ?: "Unknown error"}"
            }
            android.util.Log.e("MessagingRepo", errorMsg)
            throw IllegalStateException(errorMsg)
        }
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
        
        // Add contact - ensure all required fields are present
        val contactName = resolveContactName(contactId)
        val contact = JSONObject().apply {
            put("id", contactId)
            put("name", contactName ?: "Contact")
            put("role", "contact")
        }
        participants.put(contact)
        
        // Ensure JSON string is never null (defensive check for unit tests)
        return try {
            val jsonString = participants.toString()
            jsonString ?: """[{"id":"current_user","name":"Me","role":"current"},{"id":"$contactId","name":"${contactName ?: "Contact"}","role":"contact"}]"""
        } catch (e: Exception) {
            // Fallback JSON if toString() fails in unit tests
            """[{"id":"current_user","name":"Me","role":"current"},{"id":"$contactId","name":"${contactName ?: "Contact"}","role":"contact"}]"""
        }
    }

    // Extension functions for mapping between domain and data models
    private fun ConversationEntity.toDomain(): Conversation {
        val participants = runCatching {
            val jsonArray = JSONArray(participantsJson)
            (0 until jsonArray.length()).map { i ->
                val participant = jsonArray.getJSONObject(i)
                Participant(
                    id = participant.optString("id", "unknown"),
                    name = participant.optString("name", "Contact"),
                    avatarUrl = participant.optString("avatarUrl").takeIf { it.isNotEmpty() },
                    role = when (participant.optString("role", "group").lowercase()) {
                        "parent" -> ConversationType.PARENT
                        "educator" -> ConversationType.EDUCATOR
                        else -> ConversationType.GROUP
                    }
                )
            }
        }.getOrElse { e ->
            android.util.Log.e("MessagingRepo", "Failed to parse participantsJson: ${participantsJson}", e)
            emptyList()
        }

        return Conversation(
            id = id,
            title = title,
            lastMessagePreview = lastMessagePreview,
            lastTimestamp = lastTimestamp,
            unreadCount = unreadCount,
            type = when (type.lowercase()) {
                "direct" -> ConversationType.PARENT  // Direct conversations use PARENT type for UI
                "group" -> ConversationType.GROUP
                else -> ConversationType.GROUP
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

    // Extension function for ConversationDto
    private fun fi.kidozz.app.features.messaging.data.api.ConversationDto.toEntity(): ConversationEntity {
        // Convert participants to JSON
        val participantsJson = try {
            val jsonArray = JSONArray()
            for (participant in this.participants) {
                val jsonObject = JSONObject()
                jsonObject.put("id", participant.id)
                jsonObject.put("name", participant.name)
                if (participant.avatarUrl != null) {
                    jsonObject.put("avatarUrl", participant.avatarUrl)
                }
                jsonObject.put("role", participant.role)
                jsonArray.put(jsonObject)
            }
            jsonArray.toString()
        } catch (e: Exception) {
            android.util.Log.e("MessagingRepo", "Failed to serialize participants to JSON", e)
            "[]"
        }
        
        return ConversationEntity(
            id = this.id,
            title = this.title ?: "",
            lastMessagePreview = this.last_message_preview,
            lastTimestamp = this.last_timestamp,
            unreadCount = this.unread_count,
            type = this.type.lowercase(), // Store as "direct" or "group"
            participantsJson = participantsJson
        )
    }
    
    // Extension function for MessageDto
    private fun fi.kidozz.app.features.messaging.data.api.MessageDto.toEntity(myUserId: String): MessageEntity {
        // Convert ISO 8601 string to epoch millis
        val createdAtMillis = try {
            java.time.Instant.parse(this.createdAt).toEpochMilli()
        } catch (e: Exception) {
            try {
                java.time.OffsetDateTime.parse(this.createdAt).toInstant().toEpochMilli()
            } catch (e2: Exception) {
                android.util.Log.w("MessagingRepo", "Failed to parse createdAt: ${this.createdAt}, using current time")
                System.currentTimeMillis()
            }
        }
        
        return MessageEntity(
            id = this.id,
            conversationId = this.conversationId,
            senderId = this.senderId,
            body = this.body,
            imageUrl = this.imageUrl,
            createdAt = createdAtMillis,
            isMine = this.senderId == myUserId,
            status = "sent" // Backend doesn't provide status, default to "sent"
        )
    }
    
    // Extension function for MessageDto toDomain (for SSE events)
    // Note: Build verified successful after fixing field name mismatches (camelCase DTO properties)
    private fun fi.kidozz.app.features.messaging.data.api.MessageDto.toDomain(myUserId: String): Message {
        // Convert ISO 8601 string to epoch millis
        val createdAtMillis = try {
            java.time.Instant.parse(this.createdAt).toEpochMilli()
        } catch (e: Exception) {
            try {
                java.time.OffsetDateTime.parse(this.createdAt).toInstant().toEpochMilli()
            } catch (e2: Exception) {
                // Fallback to current time if parsing fails
                android.util.Log.w("MessagingRepo", "Failed to parse createdAt: ${this.createdAt}, using current time")
                System.currentTimeMillis()
            }
        }
        
        return Message(
            id = this.id,
            conversationId = this.conversationId,
            senderId = this.senderId,
            body = this.body,
            imageUrl = this.imageUrl,
            createdAt = createdAtMillis,
            isMine = this.senderId == myUserId,
            status = DeliveryStatus.SENT  // Backend doesn't provide status, default to SENT for server responses
        )
    }

    /**
     * Start listening to SSE events and upsert messages into local database.
     * 
     * @param scope CoroutineScope to launch the collection in
     * @param myUserId Current user ID to determine if messages are "mine"
     */
    fun startRealtime(scope: CoroutineScope, myUserId: String) {
        scope.launch {
            sseClient.eventsFlow()
                .catch { e ->
                    android.util.Log.e("MessagingRepo", "Error in SSE stream", e)
                }
                .collect { eventJson ->
                    try {
                        // Parse JSON into MessagingEventDto
                        val gson = Gson()
                        val event = gson.fromJson(eventJson, MessagingEventDto::class.java)
                        
                        // Handle "message.created" events
                        if (event.type == "message.created") {
                            // Convert MessageDto to Message domain model
                            val message = event.message.toDomain(myUserId)
                            
                            // Convert Message to MessageEntity
                            val messageEntity = message.toEntity()
                            
                            // Upsert into database (insert or replace)
                            dao.insertMessage(messageEntity)
                            
                            android.util.Log.d("MessagingRepo", "Upserted message from SSE: ${messageEntity.id} in conversation ${messageEntity.conversationId}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MessagingRepo", "Failed to parse or process SSE event", e)
                    }
                }
        }
    }
}
