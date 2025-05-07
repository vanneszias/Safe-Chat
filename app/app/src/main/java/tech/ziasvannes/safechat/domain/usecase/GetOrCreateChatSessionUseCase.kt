package tech.ziasvannes.safechat.domain.usecase

import java.util.UUID
import javax.inject.Inject
import tech.ziasvannes.safechat.data.models.ChatSession
import tech.ziasvannes.safechat.domain.repository.MessageRepository

class GetOrCreateChatSessionUseCase
@Inject
constructor(private val messageRepository: MessageRepository) {
    suspend operator fun invoke(contactId: UUID): ChatSession {
        return messageRepository.getOrCreateChatSessionForContact(contactId)
    }
}
