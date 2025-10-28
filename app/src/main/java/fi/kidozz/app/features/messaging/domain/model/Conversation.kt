package fi.kidozz.app.features.messaging.domain.model

data class Conversation(
    val id: String,
    val title: String,
    val lastMessagePreview: String?,
    val lastTimestamp: Long,
    val unreadCount: Int,
    val type: ConversationType,
    val participants: List<Participant>
)