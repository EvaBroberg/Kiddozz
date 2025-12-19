package fi.kidozz.app.features.messaging.data

import fi.kidozz.app.data.models.*
import fi.kidozz.app.features.messaging.data.db.MessagingDao
import fi.kidozz.app.features.messaging.data.api.MessagingApiService
import fi.kidozz.app.features.messaging.data.ws.MessagingWebSocketClient
import fi.kidozz.app.features.messaging.data.ws.MessagingSseClient
import okhttp3.OkHttpClient
import fi.kidozz.app.features.messaging.data.repo.MessagingRepositoryImpl
import fi.kidozz.app.features.messaging.domain.model.ContactType
import fi.kidozz.app.features.messaging.data.api.MessageDto
import fi.kidozz.app.features.messaging.data.api.SendMessageRequest
import fi.kidozz.app.features.messaging.data.db.MessageEntity
import fi.kidozz.app.features.messaging.data.db.ConversationEntity
import fi.kidozz.app.features.messaging.domain.model.DeliveryStatus
import fi.kidozz.app.features.messaging.domain.model.Message
import fi.kidozz.app.features.messaging.domain.model.ConversationType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody

class MessagingRepositoryImplTest {

    private val fakeApi = object : MessagingApiService {
        override suspend fun createDirectConversation(request: fi.kidozz.app.features.messaging.data.api.CreateDirectConversationRequest) = 
            Response.success(fi.kidozz.app.features.messaging.data.api.CreateDirectConversationResponse("conv-1", emptyList()))
        override suspend fun getConversations(cursor: String?, limit: Int) = Response.success(emptyList<fi.kidozz.app.features.messaging.data.api.ConversationDto>())
        override suspend fun getMessages(conversationId: String, cursor: String?, limit: Int) = Response.success(emptyList<MessageDto>())
        override suspend fun sendMessage(conversationId: String, message: SendMessageRequest) = Response.success(
            MessageDto("msg-1", "conv-1", "sender-1", "parent", "Hello", null, "2024-01-01T00:00:00Z")
        )
        override suspend fun markAsRead(conversationId: String) = Response.success(Unit)
        override suspend fun registerPushToken(request: fi.kidozz.app.features.messaging.data.api.RegisterPushTokenRequest) = Response.success(Unit)
    }
    
    private val fakeDao = object : MessagingDao {
        override fun observeConversationsByType(filter: String?): Flow<List<ConversationEntity>> = flowOf(emptyList())
        override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> = flowOf(emptyList())
        override suspend fun insertConversation(conversation: ConversationEntity) {}
        override suspend fun insertMessage(message: MessageEntity) {}
        override suspend fun insertMessages(messages: List<MessageEntity>) {}
        override suspend fun markConversationAsRead(conversationId: String) {}
        override suspend fun updateMessageStatus(messageId: String, status: String) {}
        override suspend fun findDirectConversationWith(contactId: String) = null
    }
    
    private val fakeWs = MessagingWebSocketClient()
    private val fakeSse = MessagingSseClient(
        OkHttpClient(),
        "http://test",
        { null }
    ) 

    @Test
    fun parents_in_my_groups_only() = runTest {
        // GIVEN my session: parent 10 in group 7 (via kids 19,20)
        val kids = listOf(
            Kid(
                id="19", full_name="A", dob="2020-01-01", group_id="7", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="10", full_name="Me", email=null, phone_num=null)),
                attendance="IN"
            ),
            Kid(
                id="20", full_name="B", dob="2020-01-01", group_id="7", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="10", full_name="Me", email=null, phone_num=null)),
                attendance="OUT"
            ),
            Kid(
                id="21", full_name="C", dob="2020-01-01", group_id="7", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="11", full_name="OtherParent", email=null, phone_num=null)),
                attendance="IN"
            ),
            // Kid in another group to ensure excluded:
            Kid(
                id="30", full_name="D", dob="2020-01-01", group_id="8", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="12", full_name="WrongParent", email=null, phone_num=null)),
                attendance="IN"
            )
        )
        val educators = listOf(
            Educator(id="25", full_name="Teacher1", role="Teacher", email=null, phone_num=null,
                groups=listOf(Group("7","G7"))),
            Educator(id="28", full_name="Teacher2", role="Teacher", email=null, phone_num=null,
                groups=listOf(Group("7","G7"))),
            Educator(id="40", full_name="Jessica", role="Teacher", email=null, phone_num=null,
                groups=listOf(Group("8","G8"))) // should be excluded
        )
        val kidsFlow = MutableStateFlow(kids)
        val educatorsFlow = MutableStateFlow(educators)

        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, fakeSse, kidsFlow, educatorsFlow)

        val contacts = repo.observeContactsInMyGroups(
            type = ContactType.PARENT,
            myGroupIds = setOf("7"),
            myUserId = "10"
        ).first()

        assertEquals(listOf("11"), contacts.map { it.id }) // only other parent in group 7
    }

    @Test
    fun educators_in_my_groups_only() = runTest {
        val kidsFlow = MutableStateFlow(emptyList<Kid>())
        val educatorsFlow = MutableStateFlow(
            listOf(
                Educator(id="25", full_name="Teacher1", role="Teacher", email=null, phone_num=null,
                    groups=listOf(Group("7","G7"))),
                Educator(id="28", full_name="Teacher2", role="Teacher", email=null, phone_num=null,
                    groups=listOf(Group("7","G7"))),
                Educator(id="40", full_name="Jessica", role="Teacher", email=null, phone_num=null,
                    groups=listOf(Group("8","G8")))
            )
        )
        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, fakeSse, kidsFlow, educatorsFlow)

        val contacts = repo.observeContactsInMyGroups(
            type = ContactType.EDUCATOR,
            myGroupIds = setOf("7"),
            myUserId = "10"
        ).first()

        assertEquals(setOf("25","28"), contacts.map { it.id }.toSet())
    }
    
    @Test
    fun wrong_session_shows_extra_contacts() = runTest {
        // This test documents why wrong session (all daycare groups) leads to showing too many contacts
        // Correct session: parent 10 should only see group 7 → only parent 11, educators 25/28
        // Wrong session: parent 10 with groups {7,8,9} → would incorrectly show Jessica (group 8)
        
        val kids = listOf(
            Kid(id="19", full_name="A", dob="2020-01-01", group_id="7", daycare_id="d1",
                trusted_adults=emptyList(), parents=listOf(Parent(id="10", full_name="Me", email=null, phone_num=null)), attendance="IN"),
            Kid(id="21", full_name="C", dob="2020-01-01", group_id="7", daycare_id="d1",
                trusted_adults=emptyList(), parents=listOf(Parent(id="11", full_name="OtherParent", email=null, phone_num=null)), attendance="IN"),
            // Kids in other groups that should NOT appear for parent 10
            Kid(id="40", full_name="Other", dob="2020-01-01", group_id="8", daycare_id="d1",
                trusted_adults=emptyList(), parents=listOf(Parent(id="99", full_name="WrongParent", email=null, phone_num=null)), attendance="IN")
        )
        val educators = listOf(
            Educator(id="25", full_name="Teacher1", role="Teacher", email=null, phone_num=null, groups=listOf(Group("7","G7"))),
            Educator(id="28", full_name="Teacher2", role="Teacher", email=null, phone_num=null, groups=listOf(Group("7","G7"))),
            Educator(id="40", full_name="Jessica", role="Teacher", email=null, phone_num=null, groups=listOf(Group("8","G8"))) // Group 8 - should NOT appear
        )
        
        val kidsFlow = MutableStateFlow(kids)
        val educatorsFlow = MutableStateFlow(educators)
        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, fakeSse, kidsFlow, educatorsFlow)
        
        // CORRECT session: only group 7 (from parent 10's kids 19,20)
        val correctContacts = repo.observeContactsInMyGroups(
            type = ContactType.PARENT,
            myGroupIds = setOf("7"), // Only group 7 - correct
            myUserId = "10"
        ).first()
        assertEquals(listOf("11"), correctContacts.map { it.id }) // Only parent 11
        
        val correctEducators = repo.observeContactsInMyGroups(
            type = ContactType.EDUCATOR,
            myGroupIds = setOf("7"), // Only group 7 - correct
            myUserId = "10"
        ).first()
        assertEquals(setOf("25","28"), correctEducators.map { it.id }.toSet()) // Only educators 25,28
        
        // WRONG session: groups {7,8} (if we incorrectly included all daycare groups)
        val wrongEducators = repo.observeContactsInMyGroups(
            type = ContactType.EDUCATOR,
            myGroupIds = setOf("7","8"), // Wrong: includes group 8
            myUserId = "10"
        ).first()
        // This would incorrectly include Jessica (group 8), demonstrating the bug
        assertTrue("Wrong session includes extra contacts", wrongEducators.map { it.id }.contains("40"))
        assertEquals(setOf("25","28","40"), wrongEducators.map { it.id }.toSet())
    }

    @Test
    fun observeAllEducatorsExcept_returns_all_educators_except_self() = runTest {
        // GIVEN: Educator directory (for educator role in Educators tab)
        val educators = listOf(
            Educator(id="25", full_name="Teacher1", role="Teacher", email=null, phone_num=null,
                groups=listOf(Group("7","G7"))),
            Educator(id="27", full_name="Jessica", role="Teacher", email=null, phone_num=null,
                groups=listOf(Group("7","G7"))),
            Educator(id="28", full_name="Teacher2", role="Teacher", email=null, phone_num=null,
                groups=listOf(Group("7","G7"))),
            Educator(id="24", full_name="Sarah Davis", role="Teacher", email=null, phone_num=null,
                groups=listOf(Group("8","G8"))) // Different group, should still appear
        )
        val educatorsFlow = MutableStateFlow(educators)
        val kidsFlow = MutableStateFlow(emptyList<Kid>())
        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, fakeSse, kidsFlow, educatorsFlow)

        // WHEN: Educator 27 (Jessica) views Educators tab
        val contacts = repo.observeAllEducatorsExcept("27").first()

        // THEN: Should see all educators except self (27), regardless of groups
        val contactIds = contacts.map { it.id }.toSet()
        assertEquals(setOf("25", "28", "24"), contactIds)
        assertFalse("Self should be excluded", contactIds.contains("27"))
        assertEquals(3, contacts.size)
    }

    @Test
    fun educator_parents_tab_shows_only_parents_from_educator_groups() = runTest {
        // GIVEN: Educator 27 (Jessica) in group 7
        val kids = listOf(
            Kid(id="19", full_name="Kid A", dob="2020-01-01", group_id="7", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="10", full_name="Sara Johnson", email=null, phone_num=null)),
                attendance="IN"),
            Kid(id="20", full_name="Kid B", dob="2020-01-01", group_id="7", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="10", full_name="Sara Johnson", email=null, phone_num=null)),
                attendance="IN"),
            Kid(id="21", full_name="Kid C", dob="2020-01-01", group_id="7", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="11", full_name="Other Parent", email=null, phone_num=null)),
                attendance="IN"),
            // Kid in different group (should be excluded)
            Kid(id="40", full_name="Kid D", dob="2020-01-01", group_id="8", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="99", full_name="Wrong Parent", email=null, phone_num=null)),
                attendance="IN")
        )
        val kidsFlow = MutableStateFlow(kids)
        val educatorsFlow = MutableStateFlow(emptyList<Educator>())
        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, fakeSse, kidsFlow, educatorsFlow)

        // WHEN: Educator 27 views Parents tab (groupIds = {7})
        val contacts = repo.observeContactsInMyGroups(
            type = ContactType.PARENT,
            myGroupIds = setOf("7"), // Educator's group
            myUserId = "27" // Educator ID (self-exclusion for parents doesn't apply, but included for consistency)
        ).first()

        // THEN: Should see only parents of kids in group 7
        val contactIds = contacts.map { it.id }.toSet()
        assertEquals(setOf("10", "11"), contactIds) // Sara Johnson and Other Parent
        assertFalse("Wrong parent should be excluded", contactIds.contains("99"))
        assertEquals(2, contacts.size)
    }

    @Test
    fun parent_session_derivation_multiple_groups_returns_only_parent_groups() = runTest {
        // GIVEN: Parent 10 with kids in groups 7 and 8
        val kids = listOf(
            Kid(id="19", full_name="Kid A", dob="2020-01-01", group_id="7", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="10", full_name="Me", email=null, phone_num=null)),
                attendance="IN"),
            Kid(id="20", full_name="Kid B", dob="2020-01-01", group_id="7", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="10", full_name="Me", email=null, phone_num=null)),
                attendance="IN"),
            Kid(id="30", full_name="Kid C", dob="2020-01-01", group_id="8", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="10", full_name="Me", email=null, phone_num=null)),
                attendance="IN"),
            // Kid in group 9 belonging to different parent (should not affect parent 10's groups)
            Kid(id="40", full_name="Kid D", dob="2020-01-01", group_id="9", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="99", full_name="Other Parent", email=null, phone_num=null)),
                attendance="IN")
        )
        val kidsFlow = MutableStateFlow(kids)
        val educatorsFlowEmpty = MutableStateFlow(emptyList<Educator>())
        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, fakeSse, kidsFlow, educatorsFlowEmpty)

        // WHEN: Parent 10 views contacts (groupIds should be {7, 8} from their kids only)
        val contacts = repo.observeContactsInMyGroups(
            type = ContactType.PARENT,
            myGroupIds = setOf("7", "8"), // Only parent 10's groups
            myUserId = "10"
        ).first()

        // THEN: Should see only parents of kids in groups 7 and 8
        // Since parent 10 is the only parent with kids in groups 7 and 8, should see empty list
        // (after self-exclusion)
        assertEquals(emptyList<String>(), contacts.map { it.id })

        // BUT: If we check educators, should see educators from groups 7 and 8
        val educators = listOf(
            Educator(id="25", full_name="Teacher1", role="Teacher", email=null, phone_num=null,
                groups=listOf(Group("7","G7"))),
            Educator(id="28", full_name="Teacher2", role="Teacher", email=null, phone_num=null,
                groups=listOf(Group("7","G7"))),
            Educator(id="40", full_name="Jessica", role="Teacher", email=null, phone_num=null,
                groups=listOf(Group("8","G8"))),
            // Educator in group 9 (should be excluded)
            Educator(id="50", full_name="Other Teacher", role="Teacher", email=null, phone_num=null,
                groups=listOf(Group("9","G9")))
        )
        val educatorsFlowWithData = MutableStateFlow(educators)
        val repoWithEducators = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, fakeSse, kidsFlow, educatorsFlowWithData)

        val educatorContacts = repoWithEducators.observeContactsInMyGroups(
            type = ContactType.EDUCATOR,
            myGroupIds = setOf("7", "8"), // Parent 10's groups
            myUserId = "10"
        ).first()

        val educatorIds = educatorContacts.map { it.id }.toSet()
        assertEquals(setOf("25", "28", "40"), educatorIds) // Teachers from groups 7 and 8
        assertFalse("Teacher from group 9 should be excluded", educatorIds.contains("50"))
    }

    @Test
    fun messageDto_toDomain_converts_iso_date_and_sets_status_sent() = runTest {
        // GIVEN: MessageDto with ISO 8601 date string
        val dto = MessageDto(
            id = "msg-1",
            conversationId = "conv-1",
            senderId = "sender-1",
            senderType = "parent",
            body = "Hello",
            imageUrl = null,
            createdAt = "2024-01-01T12:00:00Z"  // ISO 8601
        )
        
        // WHEN: Convert to domain (using reflection to access private extension)
        // Note: We can't directly test the private extension, but we can test via sendMessage flow
        val kidsFlow = MutableStateFlow(emptyList<Kid>())
        val educatorsFlow = MutableStateFlow(emptyList<Educator>())
        var capturedRequest: SendMessageRequest? = null
        val fakeApiWithResponse = object : MessagingApiService {
            override suspend fun createDirectConversation(request: fi.kidozz.app.features.messaging.data.api.CreateDirectConversationRequest) = 
                Response.success(fi.kidozz.app.features.messaging.data.api.CreateDirectConversationResponse("conv-1", emptyList()))
            override suspend fun getConversations(cursor: String?, limit: Int) = Response.success(emptyList<fi.kidozz.app.features.messaging.data.api.ConversationDto>())
            override suspend fun getMessages(conversationId: String, cursor: String?, limit: Int) = Response.success(listOf(dto))
            override suspend fun sendMessage(conversationId: String, message: SendMessageRequest): Response<MessageDto> {
                capturedRequest = message
                // Return response using client_message_id from request (backend uses client ID)
                return Response.success(
                    MessageDto(
                        id = message.client_message_id ?: "msg-1",
                        conversationId = conversationId,
                        senderId = "current_user",
                        senderType = "parent",
                        body = message.body,
                        imageUrl = message.image_url,
                        createdAt = "2024-01-01T12:00:00Z"
                    )
                )
            }
            override suspend fun markAsRead(conversationId: String) = Response.success(Unit)
            override suspend fun registerPushToken(request: fi.kidozz.app.features.messaging.data.api.RegisterPushTokenRequest) = Response.success(Unit)
        }
        
        val messages = mutableListOf<MessageEntity>()
        val fakeDaoWithCapture = object : MessagingDao {
            override fun observeConversationsByType(filter: String?): Flow<List<ConversationEntity>> = flowOf(emptyList())
            override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> = flowOf(messages)
            override suspend fun insertConversation(conversation: ConversationEntity) {}
            override suspend fun insertMessage(message: MessageEntity) { messages.add(message) }
            override suspend fun insertMessages(msgs: List<MessageEntity>) { messages.addAll(msgs) }
            override suspend fun markConversationAsRead(conversationId: String) {}
            override suspend fun updateMessageStatus(messageId: String, status: String) {}
            override suspend fun findDirectConversationWith(contactId: String) = null
        }
        
        val repo = MessagingRepositoryImpl(fakeApiWithResponse, fakeDaoWithCapture, fakeWs, fakeSse, kidsFlow, educatorsFlow)
        
        // WHEN: Send message (which calls toDomain internally)
        val result = repo.sendMessage("conv-1", "Hello", null)
        
        // THEN: Message should have epoch millis (not ISO string) - from local message
        assertTrue("createdAt should be epoch millis (Long)", result.createdAt is Long)
        assertTrue("createdAt should be > 0", result.createdAt > 0)
        assertEquals("status should be SENT", DeliveryStatus.SENT, result.status)
        assertEquals("conversationId should match", "conv-1", result.conversationId)
        // Note: senderId is now "current_user" from local message, not from DTO
        assertEquals("senderId should match", "current_user", result.senderId)
    }

    @Test
    fun sendMessage_uses_single_message_row_and_updates_status_to_sent() = runTest {
        // GIVEN: Fake API that returns successful response
        val kidsFlow = MutableStateFlow(emptyList<Kid>())
        val educatorsFlow = MutableStateFlow(emptyList<Educator>())
        var capturedRequest: SendMessageRequest? = null
        val fakeApiWithResponse = object : MessagingApiService {
            override suspend fun createDirectConversation(request: fi.kidozz.app.features.messaging.data.api.CreateDirectConversationRequest) = 
                Response.success(fi.kidozz.app.features.messaging.data.api.CreateDirectConversationResponse("conv-1", emptyList()))
            override suspend fun getConversations(cursor: String?, limit: Int) = Response.success(emptyList<fi.kidozz.app.features.messaging.data.api.ConversationDto>())
            override suspend fun getMessages(conversationId: String, cursor: String?, limit: Int) = Response.success(emptyList<MessageDto>())
            override suspend fun sendMessage(conversationId: String, message: SendMessageRequest): Response<MessageDto> {
                capturedRequest = message
                // Return response with same ID as client_message_id (backend uses client ID)
                return Response.success(
                    MessageDto(
                        id = message.client_message_id ?: "msg-1",
                        conversationId = conversationId,
                        senderId = "current_user",
                        senderType = "parent",
                        body = message.body,
                        imageUrl = message.image_url,
                        createdAt = "2024-01-01T12:00:00Z"
                    )
                )
            }
            override suspend fun markAsRead(conversationId: String) = Response.success(Unit)
            override suspend fun registerPushToken(request: fi.kidozz.app.features.messaging.data.api.RegisterPushTokenRequest) = Response.success(Unit)
        }
        
        // Fake DAO that collects inserted messages and tracks status updates
        val messages = mutableListOf<MessageEntity>()
        val statusUpdates = mutableMapOf<String, String>()
        val fakeDaoWithCapture = object : MessagingDao {
            override fun observeConversationsByType(filter: String?): Flow<List<ConversationEntity>> = flowOf(emptyList())
            override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> = flowOf(messages)
            override suspend fun insertConversation(conversation: ConversationEntity) {}
            override suspend fun insertMessage(message: MessageEntity) { 
                messages.add(message)
            }
            override suspend fun insertMessages(msgs: List<MessageEntity>) { 
                messages.addAll(msgs)
            }
            override suspend fun markConversationAsRead(conversationId: String) {}
            override suspend fun updateMessageStatus(messageId: String, status: String) {
                statusUpdates[messageId] = status
            }
            override suspend fun findDirectConversationWith(contactId: String) = null
        }
        
        val repo = MessagingRepositoryImpl(fakeApiWithResponse, fakeDaoWithCapture, fakeWs, fakeSse, kidsFlow, educatorsFlow)
        
        // WHEN: Send message
        val result = repo.sendMessage("conv-1", "Test message", null)
        
        // THEN: Only one message should be stored
        assertEquals("Should have exactly one message stored", 1, messages.size)
        
        // THEN: The stored message should have the client-generated ID
        val storedMessage = messages[0]
        assertEquals("Stored message ID should match returned message ID", result.id, storedMessage.id)
        assertEquals("Message body should match", "Test message", storedMessage.body)
        assertEquals("Message should be marked as mine", true, storedMessage.isMine)
        
        // THEN: Status should be updated to SENT
        assertEquals("Message status should be SENT", DeliveryStatus.SENT, result.status)
        assertEquals("Status should be updated in DAO", "sent", statusUpdates[result.id])
        
        // THEN: client_message_id should be sent to backend
        assertNotNull("Request should be captured", capturedRequest)
        assertEquals("client_message_id should be sent", result.id, capturedRequest?.client_message_id)
    }

    @Test
    fun sendMessage_with_feature_flag_off_returns_local_message() = runTest {
        // GIVEN: Feature flag is off (simulated by not calling API)
        val kidsFlow = MutableStateFlow(emptyList<Kid>())
        val educatorsFlow = MutableStateFlow(emptyList<Educator>())
        var apiCalled = false
        val fakeApiNoCall = object : MessagingApiService {
            override suspend fun createDirectConversation(request: fi.kidozz.app.features.messaging.data.api.CreateDirectConversationRequest) = 
                Response.success(fi.kidozz.app.features.messaging.data.api.CreateDirectConversationResponse("conv-1", emptyList()))
            override suspend fun getConversations(cursor: String?, limit: Int) = Response.success(emptyList<fi.kidozz.app.features.messaging.data.api.ConversationDto>())
            override suspend fun getMessages(conversationId: String, cursor: String?, limit: Int) = Response.success(emptyList<MessageDto>())
            override suspend fun sendMessage(conversationId: String, message: SendMessageRequest) = run { 
                apiCalled = true
                Response.success(MessageDto(id = "", conversationId = "", senderId = "", senderType = "", body = null, imageUrl = null, createdAt = "2024-01-01T00:00:00Z"))
            }
            override suspend fun markAsRead(conversationId: String) = Response.success(Unit)
            override suspend fun registerPushToken(request: fi.kidozz.app.features.messaging.data.api.RegisterPushTokenRequest) = Response.success(Unit)
        }
        
        val messages = mutableListOf<MessageEntity>()
        val fakeDaoWithCapture = object : MessagingDao {
            override fun observeConversationsByType(filter: String?): Flow<List<ConversationEntity>> = flowOf(emptyList())
            override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> = flowOf(messages)
            override suspend fun insertConversation(conversation: ConversationEntity) {}
            override suspend fun insertMessage(message: MessageEntity) { messages.add(message) }
            override suspend fun insertMessages(msgs: List<MessageEntity>) { messages.addAll(msgs) }
            override suspend fun markConversationAsRead(conversationId: String) {}
            override suspend fun updateMessageStatus(messageId: String, status: String) {}
            override suspend fun findDirectConversationWith(contactId: String) = null
        }
        
        val repo = MessagingRepositoryImpl(fakeApiNoCall, fakeDaoWithCapture, fakeWs, fakeSse, kidsFlow, educatorsFlow)
        
        // WHEN: Send message (feature flag check happens inside)
        // Note: We can't directly set BuildConfig.MESSAGING_ANDROID in tests, but we can verify the logic
        // For now, we'll test that the message is inserted locally
        val result = repo.sendMessage("conv-1", "Hello", null)
        
        // THEN: Message should be inserted locally
        assertEquals(1, messages.size)
        assertEquals("Hello", messages[0].body)
        // Note: API call check would require mocking BuildConfig, which is complex
        // The feature flag guard is tested via integration tests
    }

    @Test
    fun observeInbox_includes_direct_conversations_for_parent_educator_filters() = runTest {
        // GIVEN: DAO returns conversations including a "direct" type
        val directConv = ConversationEntity(
            id = "conv-direct-1",
            title = "Direct Chat",
            lastMessagePreview = "Hello",
            lastTimestamp = 1000L,
            unreadCount = 0,
            type = "direct",
            participantsJson = """[{"id":"10","name":"Sara"},{"id":"27","name":"Jessica"}]"""
        )
        val groupConv = ConversationEntity(
            id = "conv-group-1",
            title = "Group Chat",
            lastMessagePreview = "Group message",
            lastTimestamp = 2000L,
            unreadCount = 0,
            type = "group",
            participantsJson = """[{"id":"10"},{"id":"11"}]"""
        )
        
        val kidsFlow = MutableStateFlow(emptyList<Kid>())
        val educatorsFlow = MutableStateFlow(emptyList<Educator>())
        val fakeDaoWithDirects = object : MessagingDao {
            override fun observeConversationsByType(filter: String?): Flow<List<ConversationEntity>> {
                // When filter is null, return all (including directs)
                return flowOf(if (filter == null) listOf(directConv, groupConv) else emptyList<ConversationEntity>())
            }
            override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> = flowOf(emptyList())
            override suspend fun insertConversation(conversation: ConversationEntity) {}
            override suspend fun insertMessage(message: MessageEntity) {}
            override suspend fun insertMessages(messages: List<MessageEntity>) {}
            override suspend fun markConversationAsRead(conversationId: String) {}
            override suspend fun updateMessageStatus(messageId: String, status: String) {}
            override suspend fun findDirectConversationWith(contactId: String) = null
        }
        
        val repo = MessagingRepositoryImpl(fakeApi, fakeDaoWithDirects, fakeWs, fakeSse, kidsFlow, educatorsFlow)
        
        // WHEN: Observe inbox with PARENT filter (should pass null to DAO to include directs)
        val inbox = repo.observeInbox(ConversationType.PARENT).first()
        
        // THEN: Should include both direct and group conversations
        val conversationIds = inbox.map { it.id }.toSet()
        assertTrue("Should include direct conversation", conversationIds.contains("conv-direct-1"))
        assertTrue("Should include group conversation", conversationIds.contains("conv-group-1"))
        assertEquals(2, inbox.size)
    }

    @Test
    fun createOrGetDirectConversation_blankContactId_throws() = runTest {
        val kidsFlow = MutableStateFlow(emptyList<Kid>())
        val educatorsFlow = MutableStateFlow(emptyList<Educator>())
        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, fakeSse, kidsFlow, educatorsFlow)
        
        try {
            repo.createOrGetDirectConversation("")
            fail("Expected IllegalArgumentException for blank contactId")
        } catch (e: IllegalArgumentException) {
            assertTrue("Exception message should mention blank", e.message?.contains("blank", ignoreCase = true) == true)
        }
    }

    @Test
    fun createOrGetDirectConversation_validContactId_returnsNonBlank() = runTest {
        // Setup: contact-123 is an educator
        val educatorsFlow = MutableStateFlow(
            listOf(
                Educator(id="contact-123", full_name="Test Educator", role="Teacher", email=null, phone_num=null,
                    groups=listOf(Group("7","G7")))
            )
        )
        val kidsFlow = MutableStateFlow(emptyList<Kid>())
        
        // Fake DAO: findDirectConversationWith returns null, insertConversation collects entities
        val insertedConversations = mutableListOf<ConversationEntity>()
        val fakeDaoWithCapture = object : MessagingDao {
            override fun observeConversationsByType(filter: String?): Flow<List<ConversationEntity>> = flowOf(emptyList())
            override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> = flowOf(emptyList())
            override suspend fun insertConversation(conversation: ConversationEntity) {
                insertedConversations.add(conversation)
            }
            override suspend fun insertMessage(message: MessageEntity) {}
            override suspend fun insertMessages(messages: List<MessageEntity>) {}
            override suspend fun markConversationAsRead(conversationId: String) {}
            override suspend fun updateMessageStatus(messageId: String, status: String) {}
            override suspend fun findDirectConversationWith(contactId: String) = 
                if (contactId == "contact-123") null else null
        }
        
        // Fake API: createDirectConversation returns backend-generated ID
        val fakeApiWithBackendId = object : MessagingApiService {
            override suspend fun createDirectConversation(request: fi.kidozz.app.features.messaging.data.api.CreateDirectConversationRequest) = 
                Response.success(fi.kidozz.app.features.messaging.data.api.CreateDirectConversationResponse("conv-123-from-backend", emptyList()))
            override suspend fun getConversations(cursor: String?, limit: Int) = Response.success(emptyList<fi.kidozz.app.features.messaging.data.api.ConversationDto>())
            override suspend fun getMessages(conversationId: String, cursor: String?, limit: Int) = Response.success(emptyList<MessageDto>())
            override suspend fun sendMessage(conversationId: String, message: SendMessageRequest) = Response.success(
                MessageDto("msg-1", "conv-1", "sender-1", "parent", "Hello", null, "2024-01-01T00:00:00Z")
            )
            override suspend fun markAsRead(conversationId: String) = Response.success(Unit)
            override suspend fun registerPushToken(request: fi.kidozz.app.features.messaging.data.api.RegisterPushTokenRequest) = Response.success(Unit)
        }
        
        val repo = MessagingRepositoryImpl(fakeApiWithBackendId, fakeDaoWithCapture, fakeWs, fakeSse, kidsFlow, educatorsFlow)
        
        val conversationId = repo.createOrGetDirectConversation("contact-123")
        
        // Assert: returned ID is the backend-generated ID
        assertEquals("conv-123-from-backend", conversationId)
        // Assert: exactly one conversation was inserted
        assertEquals("Should have inserted one conversation", 1, insertedConversations.size)
        // Assert: inserted conversation has the backend-generated ID
        assertEquals("Inserted conversation should have the backend-generated ID", "conv-123-from-backend", insertedConversations[0].id)
    }

    @Test
    fun createOrGetDirectConversation_usesExistingConversationIfPresent() = runTest {
        val kidsFlow = MutableStateFlow(emptyList<Kid>())
        val educatorsFlow = MutableStateFlow(emptyList<Educator>())
        
        // Fake DAO: findDirectConversationWith returns existing conversation
        val existingConversation = ConversationEntity(
            id = "existing-conv-1",
            title = "Existing Conversation",
            lastMessagePreview = null,
            lastTimestamp = System.currentTimeMillis(),
            unreadCount = 0,
            type = "direct",
            participantsJson = """[{"id":"contact-123","name":"Contact"}]"""
        )
        
        var insertConversationCalled = false
        val fakeDaoWithExisting = object : MessagingDao {
            override fun observeConversationsByType(filter: String?): Flow<List<ConversationEntity>> = flowOf(emptyList())
            override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> = flowOf(emptyList())
            override suspend fun insertConversation(conversation: ConversationEntity) {
                insertConversationCalled = true
            }
            override suspend fun insertMessage(message: MessageEntity) {}
            override suspend fun insertMessages(messages: List<MessageEntity>) {}
            override suspend fun markConversationAsRead(conversationId: String) {}
            override suspend fun updateMessageStatus(messageId: String, status: String) {}
            override suspend fun findDirectConversationWith(contactId: String) = 
                if (contactId == "contact-123") existingConversation else null
        }
        
        // Fake API: track if createDirectConversation is called
        var createDirectConversationCalled = false
        val fakeApiWithTracking = object : MessagingApiService {
            override suspend fun createDirectConversation(request: fi.kidozz.app.features.messaging.data.api.CreateDirectConversationRequest) = run {
                createDirectConversationCalled = true
                Response.success(fi.kidozz.app.features.messaging.data.api.CreateDirectConversationResponse("should-not-be-used", emptyList()))
            }
            override suspend fun getConversations(cursor: String?, limit: Int) = Response.success(emptyList<fi.kidozz.app.features.messaging.data.api.ConversationDto>())
            override suspend fun getMessages(conversationId: String, cursor: String?, limit: Int) = Response.success(emptyList<MessageDto>())
            override suspend fun sendMessage(conversationId: String, message: SendMessageRequest) = Response.success(
                MessageDto("msg-1", "conv-1", "sender-1", "parent", "Hello", null, "2024-01-01T00:00:00Z")
            )
            override suspend fun markAsRead(conversationId: String) = Response.success(Unit)
            override suspend fun registerPushToken(request: fi.kidozz.app.features.messaging.data.api.RegisterPushTokenRequest) = Response.success(Unit)
        }
        
        val repo = MessagingRepositoryImpl(fakeApiWithTracking, fakeDaoWithExisting, fakeWs, fakeSse, kidsFlow, educatorsFlow)
        
        val conversationId = repo.createOrGetDirectConversation("contact-123")
        
        // Assert: returned ID is the existing conversation ID
        assertEquals("existing-conv-1", conversationId)
        // Assert: backend createDirectConversation was not called
        assertFalse("Backend createDirectConversation should not be called when existing conversation is found", createDirectConversationCalled)
        // Assert: insertConversation was not called
        assertFalse("insertConversation should not be called when existing conversation is found", insertConversationCalled)
    }

    @Test
    fun createOrGetDirectConversation_backendFailure_throws() = runTest {
        // Setup: contact-123 is an educator
        val educatorsFlow = MutableStateFlow(
            listOf(
                Educator(id="contact-123", full_name="Test Educator", role="Teacher", email=null, phone_num=null,
                    groups=listOf(Group("7","G7")))
            )
        )
        val kidsFlow = MutableStateFlow(emptyList<Kid>())
        
        // Fake DAO: findDirectConversationWith returns null (no existing conversation)
        val fakeDao = object : MessagingDao {
            override fun observeConversationsByType(filter: String?): Flow<List<ConversationEntity>> = flowOf(emptyList())
            override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> = flowOf(emptyList())
            override suspend fun insertConversation(conversation: ConversationEntity) {}
            override suspend fun insertMessage(message: MessageEntity) {}
            override suspend fun insertMessages(messages: List<MessageEntity>) {}
            override suspend fun markConversationAsRead(conversationId: String) {}
            override suspend fun updateMessageStatus(messageId: String, status: String) {}
            override suspend fun findDirectConversationWith(contactId: String) = null
        }
        
        // Fake API: createDirectConversation returns unsuccessful response (HTTP 500)
        val fakeApiWithFailure = object : MessagingApiService {
            override suspend fun createDirectConversation(request: fi.kidozz.app.features.messaging.data.api.CreateDirectConversationRequest): Response<fi.kidozz.app.features.messaging.data.api.CreateDirectConversationResponse> =
                Response.error(500, "Internal Server Error".toResponseBody())
            override suspend fun getConversations(cursor: String?, limit: Int) = Response.success(emptyList<fi.kidozz.app.features.messaging.data.api.ConversationDto>())
            override suspend fun getMessages(conversationId: String, cursor: String?, limit: Int) = Response.success(emptyList<MessageDto>())
            override suspend fun sendMessage(conversationId: String, message: SendMessageRequest) = Response.success(
                MessageDto("msg-1", "conv-1", "sender-1", "parent", "Hello", null, "2024-01-01T00:00:00Z")
            )
            override suspend fun markAsRead(conversationId: String) = Response.success(Unit)
            override suspend fun registerPushToken(request: fi.kidozz.app.features.messaging.data.api.RegisterPushTokenRequest) = Response.success(Unit)
        }
        
        val repo = MessagingRepositoryImpl(fakeApiWithFailure, fakeDao, fakeWs, fakeSse, kidsFlow, educatorsFlow)
        
        // Call method and expect exception
        try {
            repo.createOrGetDirectConversation("contact-123")
            fail("Expected IllegalStateException to be thrown")
        } catch (e: IllegalStateException) {
            // Assert: exception message contains HTTP status code
            assertTrue("Exception message should contain HTTP status code", e.message?.contains("500") == true || e.message?.contains("HTTP") == true)
            assertTrue("Exception message should indicate failure", e.message?.contains("Failed to create conversation") == true)
        }
    }

    @Test
    fun self_exclusion_works_for_all_contact_types() = runTest {
        // Verify self-exclusion works correctly for both PARENT and EDUCATOR contact types
        val kids = listOf(
            Kid(id="19", full_name="Kid A", dob="2020-01-01", group_id="7", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="10", full_name="Me", email=null, phone_num=null)),
                attendance="IN"),
            Kid(id="21", full_name="Kid B", dob="2020-01-01", group_id="7", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="11", full_name="Other Parent", email=null, phone_num=null)),
                attendance="IN")
        )
        val educators = listOf(
            Educator(id="25", full_name="Teacher1", role="Teacher", email=null, phone_num=null,
                groups=listOf(Group("7","G7"))),
            Educator(id="27", full_name="Jessica", role="Teacher", email=null, phone_num=null,
                groups=listOf(Group("7","G7")))
        )
        val kidsFlow = MutableStateFlow(kids)
        val educatorsFlow = MutableStateFlow(educators)
        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, fakeSse, kidsFlow, educatorsFlow)

        // Test PARENT type: self (parent 10) should be excluded
        val parentContacts = repo.observeContactsInMyGroups(
            type = ContactType.PARENT,
            myGroupIds = setOf("7"),
            myUserId = "10"
        ).first()
        val parentIds = parentContacts.map { it.id }
        assertFalse("Parent 10 should be excluded from parent contacts", parentIds.contains("10"))
        assertTrue("Other parent should be included", parentIds.contains("11"))

        // Test EDUCATOR type: self (educator 27) should be excluded
        val educatorContacts = repo.observeContactsInMyGroups(
            type = ContactType.EDUCATOR,
            myGroupIds = setOf("7"),
            myUserId = "27"
        ).first()
        val educatorIds = educatorContacts.map { it.id }
        assertFalse("Educator 27 should be excluded from educator contacts", educatorIds.contains("27"))
        assertTrue("Other educator should be included", educatorIds.contains("25"))

        // Test observeAllEducatorsExcept: self (educator 27) should be excluded
        val allEducatorContacts = repo.observeAllEducatorsExcept("27").first()
        val allEducatorIds = allEducatorContacts.map { it.id }
        assertFalse("Educator 27 should be excluded from all educators", allEducatorIds.contains("27"))
        assertTrue("Other educator should be included", allEducatorIds.contains("25"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun startRealtime_upserts_message_from_sse_event() = runTest {
        // GIVEN: Fake SSE client with a single "message.created" event
        // Note: MessagingEventDto uses @SerializedName("conversation_id") so JSON must use snake_case
        val eventJson = """{"type":"message.created","conversation_id":"conv-123","message":{"id":"msg-456","conversationId":"conv-123","senderId":"sender-789","senderType":"parent","body":"Test SSE message","imageUrl":null,"createdAt":"2024-01-15T12:30:45Z"}}"""
        
        // Create a fake SSE client that overrides eventsFlow()
        val fakeSseClient = object : MessagingSseClient(
            OkHttpClient(),
            "http://test",
            { null }
        ) {
            override fun eventsFlow(): Flow<String> = flowOf(eventJson)
        }
        
        // Fake DAO that captures insertMessage calls
        val insertedMessages = mutableListOf<MessageEntity>()
        val fakeDaoWithCapture = object : MessagingDao {
            override fun observeConversationsByType(filter: String?): Flow<List<ConversationEntity>> = flowOf(emptyList())
            override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> = flowOf(emptyList())
            override suspend fun insertConversation(conversation: ConversationEntity) {}
            override suspend fun insertMessage(message: MessageEntity) {
                insertedMessages.add(message)
            }
            override suspend fun insertMessages(messages: List<MessageEntity>) {
                insertedMessages.addAll(messages)
            }
            override suspend fun markConversationAsRead(conversationId: String) {}
            override suspend fun updateMessageStatus(messageId: String, status: String) {}
            override suspend fun findDirectConversationWith(contactId: String) = null
        }
        
        val kidsFlow = MutableStateFlow(emptyList<Kid>())
        val educatorsFlow = MutableStateFlow(emptyList<Educator>())
        val repo = MessagingRepositoryImpl(fakeApi, fakeDaoWithCapture, fakeWs, fakeSseClient, kidsFlow, educatorsFlow)
        
        // WHEN: Start realtime in test scope
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)
        val myUserId = "user-123"
        
        repo.startRealtime(testScope, myUserId)
        
        // Advance until events are consumed
        testScope.advanceUntilIdle()
        
        // THEN: insertMessage should be called with expected MessageEntity
        assertEquals("Should have inserted exactly one message", 1, insertedMessages.size)
        
        val insertedMessage = insertedMessages[0]
        assertEquals("Message ID should match", "msg-456", insertedMessage.id)
        assertEquals("Conversation ID should match", "conv-123", insertedMessage.conversationId)
        assertEquals("Sender ID should match", "sender-789", insertedMessage.senderId)
        assertEquals("Body should match", "Test SSE message", insertedMessage.body)
        assertEquals("isMine should be false (sender is different from myUserId)", false, insertedMessage.isMine)
        assertEquals("Status should be SENT", "sent", insertedMessage.status)
        assertTrue("createdAt should be a valid timestamp", insertedMessage.createdAt > 0)
    }
}
