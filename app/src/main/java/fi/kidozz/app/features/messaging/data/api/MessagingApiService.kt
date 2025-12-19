package fi.kidozz.app.features.messaging.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

interface MessagingApiService {
    @POST("api/messaging/conversations/direct")
    suspend fun createDirectConversation(
        @Body request: CreateDirectConversationRequest
    ): Response<CreateDirectConversationResponse>

    @GET("api/messaging/conversations")
    suspend fun getConversations(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<ConversationDto>>

    @GET("api/messaging/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<MessageDto>>

    @POST("api/messaging/conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Body message: SendMessageRequest
    ): Response<MessageDto>

    @POST("api/messaging/conversations/{conversationId}/read")
    suspend fun markAsRead(
        @Path("conversationId") conversationId: String
    ): Response<Unit>

    @POST("api/messaging/push/register")
    suspend fun registerPushToken(
        @Body request: RegisterPushTokenRequest
    ): Response<Unit>
}

data class CreateDirectConversationRequest(
    @SerializedName("withUserType") val with_user_type: String,
    @SerializedName("withUserId") val with_user_id: String
)

data class CreateDirectConversationResponse(
    @SerializedName("conversationId") val conversation_id: String,
    val participants: List<ParticipantDto>
)

data class SendMessageRequest(
    val body: String?,
    @SerializedName("imageUrl") val image_url: String? = null,
    @SerializedName("clientMessageId") val client_message_id: String? = null
)

data class RegisterPushTokenRequest(
    val token: String
)
