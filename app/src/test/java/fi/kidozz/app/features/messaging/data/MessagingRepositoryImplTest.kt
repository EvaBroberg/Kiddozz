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
        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, kidsFlow, educatorsFlow)

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
        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, kidsFlow, educatorsFlow)

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
        val educatorsFlow = MutableStateFlow(emptyList<Educator>())
        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, kidsFlow, educatorsFlow)

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
        val educatorsFlow = MutableStateFlow(educators)
        val repoWithEducators = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, kidsFlow, educatorsFlow)

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
        val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, kidsFlow, educatorsFlow)

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
}
