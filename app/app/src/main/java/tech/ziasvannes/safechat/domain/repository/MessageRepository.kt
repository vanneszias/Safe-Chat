package tech.ziasvannes.safechat.domain.repository

import kotlinx.coroutines.flow.Flow
import tech.ziasvannes.safechat.data.models.ChatSession
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import java.util.UUID

interface MessageRepository {
    suspend fun getMessages(chatSessionId: UUID): Flow<List<Message>>
    suspend fun sendMessage(message: Message): Result<Message>
    suspend fun updateMessageStatus(messageId: UUID, status: MessageStatus)
    suspend fun deleteMessage(messageId: UUID)
    suspend fun getChatSessions(): Flow<List<ChatSession>>
}