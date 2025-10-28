package fi.kidozz.app.features.messaging.data.api

import com.google.gson.annotations.SerializedName

data class MessageDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("body")
    val body: String?,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("status")
    val status: String = "sent"
)

data class ConversationDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("last_message_preview")
    val lastMessagePreview: String?,
    @SerializedName("last_timestamp")
    val lastTimestamp: Long,
    @SerializedName("unread_count")
    val unreadCount: Int,
    @SerializedName("type")
    val type: String,
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
