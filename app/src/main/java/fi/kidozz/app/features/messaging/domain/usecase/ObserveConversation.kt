package fi.kidozz.app.features.messaging.domain.usecase

import fi.kidozz.app.features.messaging.domain.model.Message
import fi.kidozz.app.features.messaging.domain.repo.MessagingRepository
import kotlinx.coroutines.flow.Flow

class ObserveConversation(
    private val repository: MessagingRepository
) {
    operator fun invoke(conversationId: String): Flow<List<Message>> {
        return repository.observeConversation(conversationId)
    }
}