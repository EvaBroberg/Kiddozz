package fi.kidozz.app.features.messaging.domain.model

data class Participant(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val role: ConversationType
)