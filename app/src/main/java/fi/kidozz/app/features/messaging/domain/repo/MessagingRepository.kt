package fi.kidozz.app.features.messaging.domain.repo

import fi.kidozz.app.features.messaging.domain.model.Conversation
import fi.kidozz.app.features.messaging.domain.model.ConversationType
import fi.kidozz.app.features.messaging.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessagingRepository {
    fun observeInbox(filter: ConversationType?): Flow<List<Conversation>>
    fun observeConversation(conversationId: String): Flow<List<Message>>
    suspend fun sendMessage(conversationId: String, text: String?, imageBytes: ByteArray? = null): Message
    suspend fun markAsRead(conversationId: String)
    suspend fun syncInitial() // fetch first page, prime cache
}