package fi.kidozz.app.features.messaging.data.ws

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MessagingSseClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: MessagingSseClient
    private val okHttpClient = OkHttpClient()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        val baseUrl = mockWebServer.url("/").toString().removeSuffix("/")
        client = MessagingSseClient(
            okHttpClient = okHttpClient,
            baseUrl = baseUrl,
            authTokenProvider = { "test-token-123" }
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun eventsFlow_receives_sse_data_lines() = runTest {
        // GIVEN: Mock SSE stream with data lines
        val event1 = """{"type":"message.created","conversationId":"conv-1","message":{"id":"msg-1","body":"Hello"}}"""
        val event2 = """{"type":"message.created","conversationId":"conv-2","message":{"id":"msg-2","body":"World"}}"""
        
        val sseResponse = MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(
                """
                : keepalive
                
                data: $event1
                
                data: $event2
                
                """.trimIndent()
            )
        
        mockWebServer.enqueue(sseResponse)
        
        // WHEN: Collect first event from flow
        val firstEvent = client.eventsFlow().first()
        
        // THEN: Should receive one of the JSON payload lines
        assertNotNull("First event should not be null", firstEvent)
        assertTrue("First event should be JSON", firstEvent.startsWith("{"))
        assertTrue("First event should contain message.created", firstEvent.contains("message.created"))
        
        // Verify it's one of the expected events
        val isEvent1 = firstEvent == event1
        val isEvent2 = firstEvent == event2
        assertTrue("First event should match event1 or event2", isEvent1 || isEvent2)
    }
}

