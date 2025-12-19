package fi.kidozz.app.features.messaging.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessagingDao {
    @Query("SELECT * FROM conversations WHERE :filter IS NULL OR type = :filter ORDER BY lastTimestamp DESC")
    fun observeConversationsByType(filter: String?): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun markConversationAsRead(conversationId: String)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("SELECT * FROM conversations WHERE participantsJson LIKE '%' || :contactId || '%' LIMIT 1")
    suspend fun findDirectConversationWith(contactId: String): ConversationEntity?
}
