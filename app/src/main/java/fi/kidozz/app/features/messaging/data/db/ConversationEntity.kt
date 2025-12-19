package fi.kidozz.app.features.messaging.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val lastMessagePreview: String?,
    val lastTimestamp: Long,
    val unreadCount: Int,
    val type: String,              // "parent" | "educator" | "group"
    val participantsJson: String   // JSON array of participants
)
