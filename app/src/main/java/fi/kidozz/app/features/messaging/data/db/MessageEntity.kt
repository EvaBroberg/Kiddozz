package fi.kidozz.app.features.messaging.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val body: String?,
    val imageUrl: String? = null,
    val createdAt: Long,
    val isMine: Boolean,
    val status: String = "sent" // pending | sent | delivered | read
)
