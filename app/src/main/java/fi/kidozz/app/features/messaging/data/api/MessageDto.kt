package fi.kidozz.app.features.messaging.data.api

import com.google.gson.annotations.SerializedName

data class MessageDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("conversationId")
    val conversationId: String,
    @SerializedName("senderId")
    val senderId: String,
    @SerializedName("senderType")
    val senderType: String,
    @SerializedName("body")
    val body: String?,
    @SerializedName("imageUrl")
    val imageUrl: String? = null,
    @SerializedName("createdAt")
    val createdAt: String  // ISO 8601 datetime string
    // Note: status field is not provided by backend
)

data class ConversationDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("daycareId")
    val daycare_id: String,
    @SerializedName("title")
    val title: String?,
    @SerializedName("lastMessagePreview")
    val last_message_preview: String?,
    @SerializedName("lastTimestamp")
    val last_timestamp: Long,
    @SerializedName("unreadCount")
    val unread_count: Int,
    @SerializedName("participants")
    val participants: List<ParticipantDto>
)

data class ParticipantDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    @SerializedName("role")
    val role: String
)

data class MessagingEventDto(
    val type: String,
    @SerializedName("conversation_id")
    val conversationId: String,
    val message: MessageDto
)
