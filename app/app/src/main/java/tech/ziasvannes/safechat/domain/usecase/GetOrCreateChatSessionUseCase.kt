package tech.ziasvannes.safechat.domain.usecase

import android.util.Log
import java.util.UUID
import javax.inject.Inject
import tech.ziasvannes.safechat.data.models.ChatSession
import tech.ziasvannes.safechat.domain.repository.MessageRepository

class GetOrCreateChatSessionUseCase
@Inject
constructor(private val messageRepository: MessageRepository) {
    /**
     * Retrieves an existing chat session for the specified contact or creates a new one if none exists.
     *
     * @param contactId The unique identifier of the contact.
     * @return The chat session associated with the given contact.
     */
    suspend operator fun invoke(contactId: UUID): ChatSession {
        Log.d("GetOrCreateChatSessionUseCase", "invoke called with contactId: $contactId")
        return messageRepository.getOrCreateChatSessionForContact(contactId)
    }
}
