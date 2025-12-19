package fi.kidozz.app.features.messaging.data.ws

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSource
import java.io.IOException

open class MessagingSseClient(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String,
    private val authTokenProvider: () -> String?
) {
    open fun eventsFlow(): Flow<String> = flow {
        val token = authTokenProvider()
        val url = "$baseUrl/api/messaging/events/stream"
        
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
        
        // Add Authorization header if token is available
        token?.let {
            requestBuilder.header("Authorization", "Bearer $it")
        }
        
        val request = requestBuilder.build()
        var response: Response? = null
        
        try {
            response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("SSE connection failed: ${response.code} ${response.message}")
            }
            
            val responseBody = response.body
                ?: throw IOException("Response body is null")
            
            val source: BufferedSource = responseBody.source()
            
            try {
                while (true) {
                    // Read line-by-line
                    val line = source.readUtf8Line()
                        ?: break // End of stream
                    
                    // SSE format: lines starting with "data: " contain the actual data
                    if (line.startsWith("data: ")) {
                        // Extract the data part (everything after "data: ")
                        val data = line.substring(6) // "data: ".length = 6
                        emit(data)
                    }
                    // Skip other lines (comments, empty lines, etc.)
                }
            } finally {
                source.close()
            }
        } finally {
            // Ensure response body is closed on cancellation or completion
            response?.close()
        }
    }.onCompletion { cause ->
        // Log or handle completion/cancellation if needed
        if (cause != null) {
            android.util.Log.d("MessagingSseClient", "SSE stream completed with error", cause)
        }
    }
}

