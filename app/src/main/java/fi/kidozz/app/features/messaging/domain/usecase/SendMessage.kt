package fi.kidozz.app.features.messaging.domain.usecase

import fi.kidozz.app.features.messaging.domain.model.Message
import fi.kidozz.app.features.messaging.domain.repo.MessagingRepository

class SendMessage(
    private val repository: MessagingRepository
) {
    suspend operator fun invoke(conversationId: String, text: String?, imageBytes: ByteArray? = null): Message {
        return repository.sendMessage(conversationId, text, imageBytes)
    }
}