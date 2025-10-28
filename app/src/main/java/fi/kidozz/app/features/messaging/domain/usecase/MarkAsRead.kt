package fi.kidozz.app.features.messaging.domain.usecase

import fi.kidozz.app.features.messaging.domain.repo.MessagingRepository

class MarkAsRead(
    private val repository: MessagingRepository
) {
    suspend operator fun invoke(conversationId: String) {
        repository.markAsRead(conversationId)
    }
}