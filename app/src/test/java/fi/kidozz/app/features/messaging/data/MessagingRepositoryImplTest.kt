package fi.kidozz.app.features.messaging.data

import fi.kidozz.app.data.models.*
import fi.kidozz.app.features.messaging.data.db.MessagingDao
import fi.kidozz.app.features.messaging.data.api.MessagingApiService
import fi.kidozz.app.features.messaging.data.ws.MessagingWebSocketClient
import fi.kidozz.app.features.messaging.data.repo.MessagingRepositoryImpl
import fi.kidozz.app.features.messaging.domain.model.ContactType
import fi.kidozz.app.features.messaging.data.api.MessageDto
import fi.kidozz.app.features.messaging.data.api.SendMessageRequest
import fi.kidozz.app.features.messaging.data.db.MessageEntity
import fi.kidozz.app.features.messaging.data.db.ConversationEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Response

class MessagingRepositoryImplTest {

    private val fakeApi = object : MessagingApiService {
        override suspend fun getConversations(filter: String?) = Response.success(emptyList())
        override suspend fun getMessages(conversationId: String, cursor: String?) = Response.success(emptyList())
        override suspend fun sendMessage(conversationId: String, message: SendMessageRequest) = Response.success(MessageDto("", "", "", null, "", 0L, false, ""))
        override suspend fun markAsRead(conversationId: String) = Response.success(Unit)
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

        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, kidsFlow, educatorsFlow)

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
        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, kidsFlow, educatorsFlow)

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
        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, kidsFlow, educatorsFlow)
        
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
}
