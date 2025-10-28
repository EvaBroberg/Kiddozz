package fi.kidozz.app.features.messaging.data.repo

import fi.kidozz.app.features.messaging.data.api.MessagingApiService
import fi.kidozz.app.features.messaging.data.api.SendMessageRequest
import fi.kidozz.app.features.messaging.data.db.MessagingDao
import fi.kidozz.app.features.messaging.data.db.MessageEntity
import fi.kidozz.app.features.messaging.data.db.ConversationEntity
import fi.kidozz.app.features.messaging.data.ws.MessagingWebSocketClient
import fi.kidozz.app.features.messaging.domain.model.*
import fi.kidozz.app.features.messaging.domain.repo.MessagingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class MessagingRepositoryImpl(
    private val apiService: MessagingApiService,
    private val dao: MessagingDao,
    private val webSocketClient: MessagingWebSocketClient
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
                val conversations = response.body() ?: emptyList()
                // Convert and insert conversations
                // This is a simplified implementation
            }
        } catch (e: Exception) {
            // Handle error
        }
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
