package fi.kidozz.app.features.messaging.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.kidozz.app.features.messaging.domain.model.Conversation
import fi.kidozz.app.features.messaging.domain.model.ConversationType
import fi.kidozz.app.features.messaging.domain.model.Message
import fi.kidozz.app.features.messaging.domain.repo.MessagingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class MessagingViewModel(
    private val repository: MessagingRepository
) : ViewModel() {

    private val _filter = MutableStateFlow<ConversationType?>(null) // null = all
    val filter: StateFlow<ConversationType?> = _filter

    val inbox: StateFlow<List<Conversation>> =
        _filter.flatMapLatest { repository.observeInbox(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(type: ConversationType?) {
        _filter.value = type
    }

    fun conversation(conversationId: String): Flow<List<Message>> =
        repository.observeConversation(conversationId)

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

    init {
        viewModelScope.launch {
            repository.syncInitial()
        }
    }
}