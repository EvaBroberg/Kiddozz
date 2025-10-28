package fi.kidozz.app.features.messaging.domain.model

enum class DeliveryStatus { 
    PENDING, 
    SENT, 
    DELIVERED, 
    READ 
}

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val body: String?,
    val imageUrl: String? = null,
    val createdAt: Long,
    val isMine: Boolean,
    val status: DeliveryStatus = DeliveryStatus.SENT
)