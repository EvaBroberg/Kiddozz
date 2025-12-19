package fi.kidozz.app.features.messaging.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.kidozz.app.features.messaging.domain.model.Conversation
import fi.kidozz.app.features.messaging.domain.model.ConversationType
import fi.kidozz.app.features.messaging.domain.model.Message
import fi.kidozz.app.features.messaging.domain.model.Contact
import fi.kidozz.app.features.messaging.domain.model.ContactType
import fi.kidozz.app.features.messaging.domain.repo.MessagingRepository
import fi.kidozz.app.core.session.UserSessionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class MessagingViewModel(
    private val repository: MessagingRepository,
    private val sessionManager: UserSessionManager
) : ViewModel() {

    private val _filter = MutableStateFlow<ConversationType>(ConversationType.PARENT)
    val filter: StateFlow<ConversationType> = _filter

    private val session = sessionManager.session

    val inbox: StateFlow<List<Conversation>> =
        _filter.flatMapLatest { filter ->
            when (filter) {
                ConversationType.GROUP -> repository.observeInbox(ConversationType.GROUP)
                else -> flowOf(emptyList())
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val contacts: StateFlow<List<Contact>> =
        combine(_filter, session) { filter, session ->
            // Log cache sizes from repository (approximate)
            val kidsSize = (repository as? fi.kidozz.app.features.messaging.data.repo.MessagingRepositoryImpl)?.let { 
                it.kidsCache.value.size 
            } ?: 0
            val educatorsSize = (repository as? fi.kidozz.app.features.messaging.data.repo.MessagingRepositoryImpl)?.let { 
                it.educatorsCache.value.size 
            } ?: 0
            
            // TODO(LOG-REMOVE)
            android.util.Log.d("VMConv", "filter=$filter role=${session.role} groupIds=${session.groupIds}")
            android.util.Log.d("ContactsSelect", "filter=$filter, role=${session.role}, userId=${session.userId}, groupIds=${session.groupIds} (size=${session.groupIds.size}), size(kids)=$kidsSize, size(educators)=$educatorsSize")

            // Guardrail: check for placeholder user IDs
            if (session.userId == "current_user" || session.userId == "unknown_parent") {
                android.util.Log.w("MessagingViewModel", "WARNING: Using placeholder userId '${session.userId}'. Returning empty contacts list to prevent showing everyone.")
                emptyFlow<List<Contact>>()
            } else {
                when (filter) {
                    ConversationType.PARENT -> {
                        // Both roles: Show only parents who share my groupIds
                        if (session.groupIds.isEmpty()) {
                            android.util.Log.w("MessagingViewModel", "Parent filter selected but session.groupIds is empty!")
                        }
                        repository.observeContactsInMyGroups(ContactType.PARENT, session.groupIds, session.userId)
                    }
                    ConversationType.EDUCATOR -> {
                        // Role-aware branching:
                        // - Educator role: Show ALL educators in daycare except myself
                        // - Parent role: Show only educators assigned to my groupIds
                        when (session.role) {
                            fi.kidozz.app.core.session.UserRole.EDUCATOR -> {
                                android.util.Log.d("MessagingViewModel", "Educator role: showing all educators except self")
                                repository.observeAllEducatorsExcept(session.userId)
                            }
                            fi.kidozz.app.core.session.UserRole.PARENT -> {
                                android.util.Log.d("MessagingViewModel", "Parent role: showing educators from my groups")
                                if (session.groupIds.isEmpty()) {
                                    android.util.Log.w("MessagingViewModel", "Educator filter selected but session.groupIds is empty!")
                                }
                                repository.observeContactsInMyGroups(ContactType.EDUCATOR, session.groupIds, session.userId)
                            }
                        }
                    }
                    ConversationType.GROUP -> emptyFlow<List<Contact>>() // Groups use inbox, not contacts
                }
            }
        }.flatMapLatest { flow ->
            flow.onEach { contacts ->
                android.util.Log.d("MessagingViewModel", "Contacts flow emitted ${contacts.size} contacts: ${contacts.map { "${it.name} (${it.id})" }}")
            }
        }
         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(type: ConversationType) {
        _filter.value = type
    }

    fun conversation(conversationId: String): Flow<List<Message>> {
        // TODO(LOG-REMOVE)
        android.util.Log.d("VMConv", "observe conversationId=$conversationId")
        return repository.observeConversation(conversationId)
    }

    fun send(conversationId: String, text: String?, image: ByteArray? = null) {
        viewModelScope.launch {
            try {
                repository.sendMessage(conversationId, text, image)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun markRead(conversationId: String) {
        viewModelScope.launch {
            repository.markAsRead(conversationId)
        }
    }

    suspend fun openDirectWith(contactId: String): String =
        repository.createOrGetDirectConversation(contactId)

    init {
        viewModelScope.launch {
            repository.syncInitial()
        }
    }
}