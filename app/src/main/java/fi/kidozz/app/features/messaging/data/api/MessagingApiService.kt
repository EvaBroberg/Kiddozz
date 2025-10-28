package fi.kidozz.app.features.messaging.data.api

import fi.kidozz.app.features.messaging.data.api.MessageDto
import retrofit2.Response
import retrofit2.http.*

interface MessagingApiService {
    @GET("conversations")
    suspend fun getConversations(
        @Query("filter") filter: String? = null
    ): Response<List<MessageDto>>

    @GET("conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String,
        @Query("cursor") cursor: String? = null
    ): Response<List<MessageDto>>

    @POST("conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Body message: SendMessageRequest
    ): Response<MessageDto>

    @POST("conversations/{conversationId}/read")
    suspend fun markAsRead(
        @Path("conversationId") conversationId: String
    ): Response<Unit>
}

data class SendMessageRequest(
    val text: String?,
    val image_url: String? = null
)
