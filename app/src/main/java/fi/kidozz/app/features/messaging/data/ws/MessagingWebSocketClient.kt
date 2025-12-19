package fi.kidozz.app.features.messaging.data.ws

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class WebSocketEvent(
    val type: String,
    val data: JSONObject
)

class MessagingWebSocketClient {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val _events = MutableSharedFlow<WebSocketEvent>()
    val events: Flow<WebSocketEvent> = _events.asSharedFlow()

    fun connect(baseUrl: String) {
        val request = Request.Builder()
            .url("$baseUrl/ws/messages")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val event = WebSocketEvent(
                        type = json.getString("type"),
                        data = json.getJSONObject("data")
                    )
                    _events.tryEmit(event)
                } catch (e: Exception) {
                    // Handle JSON parsing error
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Handle connection failure
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                // Handle connection closed
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
    }

    fun sendTyping(conversationId: String) {
        val message = JSONObject().apply {
            put("type", "typing")
            put("conversation_id", conversationId)
        }
        webSocket?.send(message.toString())
    }
}
