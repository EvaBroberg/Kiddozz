package fi.kidozz.app.features.messaging.domain.usecase

import fi.kidozz.app.features.messaging.domain.model.Conversation
import fi.kidozz.app.features.messaging.domain.model.ConversationType
import fi.kidozz.app.features.messaging.domain.repo.MessagingRepository
import kotlinx.coroutines.flow.Flow

class ObserveInbox(
    private val repository: MessagingRepository
) {
    operator fun invoke(filter: ConversationType?): Flow<List<Conversation>> {
        return repository.observeInbox(filter)
    }
}