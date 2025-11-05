package fi.kidozz.app.features.messaging.ui

import fi.kidozz.app.core.session.UserRole
import fi.kidozz.app.core.session.UserSession
import fi.kidozz.app.core.session.UserSessionManager
import fi.kidozz.app.data.models.*
import fi.kidozz.app.features.messaging.data.db.MessagingDao
import fi.kidozz.app.features.messaging.data.api.MessagingApiService
import fi.kidozz.app.features.messaging.data.ws.MessagingWebSocketClient
import fi.kidozz.app.features.messaging.data.repo.MessagingRepositoryImpl
import fi.kidozz.app.features.messaging.domain.model.ContactType
import fi.kidozz.app.features.messaging.domain.model.ConversationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Response

class MessagingViewModelTest {

    private val fakeApi = object : MessagingApiService {
        override suspend fun getConversations(filter: String?) = Response.success(emptyList())
        override suspend fun getMessages(conversationId: String, cursor: String?) = Response.success(emptyList())
        override suspend fun sendMessage(conversationId: String, message: fi.kidozz.app.features.messaging.data.api.SendMessageRequest) = Response.success(fi.kidozz.app.features.messaging.data.api.MessageDto("", "", "", null, "", 0L, false, ""))
        override suspend fun markAsRead(conversationId: String) = Response.success(Unit)
    }
    
    private val fakeDao = object : MessagingDao {
        override fun observeConversationsByType(filter: String?): kotlinx.coroutines.flow.Flow<List<fi.kidozz.app.features.messaging.data.db.ConversationEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
        override fun observeMessages(conversationId: String): kotlinx.coroutines.flow.Flow<List<fi.kidozz.app.features.messaging.data.db.MessageEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
        override suspend fun insertConversation(conversation: fi.kidozz.app.features.messaging.data.db.ConversationEntity) {}
        override suspend fun insertMessage(message: fi.kidozz.app.features.messaging.data.db.MessageEntity) {}
        override suspend fun insertMessages(messages: List<fi.kidozz.app.features.messaging.data.db.MessageEntity>) {}
        override suspend fun markConversationAsRead(conversationId: String) {}
        override suspend fun updateMessageStatus(messageId: String, status: String) {}
        override suspend fun findDirectConversationWith(contactId: String) = null
    }
    
    private val fakeWs = MessagingWebSocketClient()

    @Test
    fun educator_educators_tab_shows_all_educators_except_self() = runTest {
        // GIVEN: Educator 27 (Jessica) logged in
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
        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, kidsFlow, educatorsFlow)
        
        val sessionManager = UserSessionManager(
            UserSession(userId = "27", role = UserRole.EDUCATOR, groupIds = setOf("7"))
        )
        val viewModel = MessagingViewModel(repo, sessionManager)

        // WHEN: Educator 27 selects Educators tab
        viewModel.setFilter(ConversationType.EDUCATOR)
        
        // Wait for flow to emit
        val contacts = viewModel.contacts.first { it.isNotEmpty() || true }

        // THEN: Should see all educators except self (27)
        val contactIds = contacts.map { it.id }.toSet()
        assertFalse("Self (Jessica 27) should be excluded", contactIds.contains("27"))
        assertTrue("Teacher1 should be included", contactIds.contains("25"))
        assertTrue("Teacher2 should be included", contactIds.contains("28"))
        assertTrue("Sarah Davis should be included (all educators)", contactIds.contains("24"))
    }

    @Test
    fun educator_parents_tab_shows_only_parents_from_educator_groups() = runTest {
        // GIVEN: Educator 27 (Jessica) in group 7
        val kids = listOf(
            Kid(id="19", full_name="Kid A", dob="2020-01-01", group_id="7", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="10", full_name="Sara Johnson", email=null, phone_num=null)),
                attendance="IN"),
            Kid(id="21", full_name="Kid B", dob="2020-01-01", group_id="7", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="11", full_name="Other Parent", email=null, phone_num=null)),
                attendance="IN"),
            // Kid in different group (should be excluded)
            Kid(id="40", full_name="Kid C", dob="2020-01-01", group_id="8", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="99", full_name="Wrong Parent", email=null, phone_num=null)),
                attendance="IN")
        )
        val kidsFlow = MutableStateFlow(kids)
        val educatorsFlow = MutableStateFlow(emptyList<Educator>())
        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, kidsFlow, educatorsFlow)
        
        val sessionManager = UserSessionManager(
            UserSession(userId = "27", role = UserRole.EDUCATOR, groupIds = setOf("7"))
        )
        val viewModel = MessagingViewModel(repo, sessionManager)

        // WHEN: Educator 27 selects Parents tab
        viewModel.setFilter(ConversationType.PARENT)
        
        // Wait for flow to emit
        val contacts = viewModel.contacts.first { it.isNotEmpty() || true }

        // THEN: Should see only parents of kids in group 7
        val contactIds = contacts.map { it.id }.toSet()
        assertTrue("Sara Johnson should be included", contactIds.contains("10"))
        assertTrue("Other Parent should be included", contactIds.contains("11"))
        assertFalse("Wrong Parent should be excluded", contactIds.contains("99"))
        assertEquals(2, contacts.size)
    }

    @Test
    fun parent_educators_tab_shows_only_educators_from_parent_groups() = runTest {
        // GIVEN: Parent 10 in group 7 (via kids 19, 20)
        val educators = listOf(
            Educator(id="25", full_name="Teacher1", role="Teacher", email=null, phone_num=null,
                groups=listOf(Group("7","G7"))),
            Educator(id="28", full_name="Teacher2", role="Teacher", email=null, phone_num=null,
                groups=listOf(Group("7","G7"))),
            Educator(id="40", full_name="Jessica", role="Teacher", email=null, phone_num=null,
                groups=listOf(Group("8","G8"))) // Different group, should be excluded
        )
        val educatorsFlow = MutableStateFlow(educators)
        val kidsFlow = MutableStateFlow(emptyList<Kid>())
        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, kidsFlow, educatorsFlow)
        
        val sessionManager = UserSessionManager(
            UserSession(userId = "10", role = UserRole.PARENT, groupIds = setOf("7"))
        )
        val viewModel = MessagingViewModel(repo, sessionManager)

        // WHEN: Parent 10 selects Educators tab
        viewModel.setFilter(ConversationType.EDUCATOR)
        
        // Wait for flow to emit
        val contacts = viewModel.contacts.first { it.isNotEmpty() || true }

        // THEN: Should see only educators assigned to group 7
        val contactIds = contacts.map { it.id }.toSet()
        assertTrue("Teacher1 should be included", contactIds.contains("25"))
        assertTrue("Teacher2 should be included", contactIds.contains("28"))
        assertFalse("Jessica should be excluded (different group)", contactIds.contains("40"))
        assertEquals(2, contacts.size)
    }

    @Test
    fun parent_parents_tab_shows_only_parents_from_parent_groups() = runTest {
        // GIVEN: Parent 10 in group 7 (via kids 19, 20)
        val kids = listOf(
            Kid(id="19", full_name="Kid A", dob="2020-01-01", group_id="7", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="10", full_name="Me", email=null, phone_num=null)),
                attendance="IN"),
            Kid(id="21", full_name="Kid B", dob="2020-01-01", group_id="7", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="11", full_name="Other Parent", email=null, phone_num=null)),
                attendance="IN"),
            // Kid in different group (should be excluded)
            Kid(id="40", full_name="Kid C", dob="2020-01-01", group_id="8", daycare_id="d1",
                trusted_adults=emptyList(),
                parents=listOf(Parent(id="99", full_name="Wrong Parent", email=null, phone_num=null)),
                attendance="IN")
        )
        val kidsFlow = MutableStateFlow(kids)
        val educatorsFlow = MutableStateFlow(emptyList<Educator>())
        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, kidsFlow, educatorsFlow)
        
        val sessionManager = UserSessionManager(
            UserSession(userId = "10", role = UserRole.PARENT, groupIds = setOf("7"))
        )
        val viewModel = MessagingViewModel(repo, sessionManager)

        // WHEN: Parent 10 selects Parents tab
        viewModel.setFilter(ConversationType.PARENT)
        
        // Wait for flow to emit
        val contacts = viewModel.contacts.first { it.isNotEmpty() || true }

        // THEN: Should see only parents who share group 7 (excluding self)
        val contactIds = contacts.map { it.id }.toSet()
        assertFalse("Self (parent 10) should be excluded", contactIds.contains("10"))
        assertTrue("Other Parent should be included", contactIds.contains("11"))
        assertFalse("Wrong Parent should be excluded", contactIds.contains("99"))
        assertEquals(1, contacts.size)
    }
}

