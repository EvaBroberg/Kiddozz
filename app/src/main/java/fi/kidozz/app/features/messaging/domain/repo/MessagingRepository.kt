package fi.kidozz.app.features.messaging.domain.repo

import fi.kidozz.app.features.messaging.domain.model.Conversation
import fi.kidozz.app.features.messaging.domain.model.ConversationType
import fi.kidozz.app.features.messaging.domain.model.Message
import fi.kidozz.app.features.messaging.domain.model.Contact
import fi.kidozz.app.features.messaging.domain.model.ContactType
import kotlinx.coroutines.flow.Flow

interface MessagingRepository {
    fun observeInbox(filter: ConversationType?): Flow<List<Conversation>>
    fun observeConversation(conversationId: String): Flow<List<Message>>
    suspend fun sendMessage(conversationId: String, text: String?, imageBytes: ByteArray? = null): Message
    suspend fun markAsRead(conversationId: String)
    suspend fun syncInitial()

    // NEW: contacts in my groups
    fun observeContactsInMyGroups(type: ContactType, myGroupIds: Set<String>, myUserId: String): Flow<List<Contact>>

    // NEW: create or reuse a direct conversation with a contact
    suspend fun createOrGetDirectConversation(contactId: String): String // returns conversationId
}