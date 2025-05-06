package tech.ziasvannes.safechat.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import tech.ziasvannes.safechat.data.models.ChatSession
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import java.util.UUID
import javax.inject.Inject

/**
 * A fake implementation of MessageRepository that provides test data for UI testing and development.
 *
 * This repository generates realistic test messages and chat sessions without requiring
 * a connection to an actual backend API.
 */
class FakeMessageRepository @Inject constructor(
    private val contactRepository: ContactRepository
) : MessageRepository {

    // Use the currentUserId from TestDataGenerator for consistency
    private val currentUserId = TestDataGenerator.currentUserId
    
    // All messages across all chats
    private val allMessages = mutableListOf<Message>()
    
    // Chat sessions for conversations
    private val chatSessions = mutableListOf<ChatSession>()
    
    // Observable flows
    private val chatSessionsFlow = MutableStateFlow<List<ChatSession>>(emptyList())
    private val messagesFlowMap = mutableMapOf<UUID, MutableStateFlow<List<Message>>>()
    
    init {
        // Initialize with test data asynchronously
        initializeTestData()
    }
    
    private fun initializeTestData() {
        // Get contacts - we'll do this synchronously for the fake implementation
        val contacts = runCatching {
            val contactsField = (contactRepository as? FakeContactRepository)
                ?.javaClass?.getDeclaredField("contacts")
            contactsField?.isAccessible = true
            contactsField?.get(contactRepository) as? MutableList<*>
        }.getOrNull()?.filterIsInstance<tech.ziasvannes.safechat.data.models.Contact>() 
            ?: TestDataGenerator.generateContacts()
        
        // Generate chat sessions and messages for each contact
        chatSessions.addAll(TestDataGenerator.generateChatSessions(currentUserId, contacts))
        
        // Populate allMessages with messages from all chat sessions
        chatSessions.forEach { session ->
            val contact = contacts.find { it.id in session.participantIds && it.id != currentUserId }
            if (contact != null) {
                val messages = TestDataGenerator.generateMessages(
                    chatSessionId = session.id,
                    currentUserId = currentUserId,
                    contactId = contact.id
                )
                allMessages.addAll(messages)
                
                // Create a flow for this chat session's messages
                messagesFlowMap[session.id] = MutableStateFlow(messages)
            }
        }
        
        // Update the chat sessions flow
        chatSessionsFlow.value = chatSessions
    }

    /**
     * Returns a flow emitting lists of messages for the specified chat session.
     */
    override suspend fun getMessages(chatSessionId: UUID): Flow<List<Message>> {
        // Create the flow if it doesn't exist
        if (!messagesFlowMap.containsKey(chatSessionId)) {
            messagesFlowMap[chatSessionId] = MutableStateFlow(
                allMessages.filter { message ->
                    val session = chatSessions.find { it.id == chatSessionId }
                    session?.participantIds?.contains(message.senderId) == true &&
                    session.participantIds.contains(message.receiverId)
                }.sortedBy { it.timestamp }
            )
        }
        
        return messagesFlowMap[chatSessionId] ?: MutableStateFlow(emptyList())
    }

    /**
     * Simulates sending a message and returns a success result with the message.
     */
    override suspend fun sendMessage(message: Message): Result<Message> {
        // Add message to appropriate lists
        allMessages.add(message)
        
        // Find the chat session for this message
        val chatSessionId = chatSessions.find { session ->
            session.participantIds.contains(message.senderId) && 
            session.participantIds.contains(message.receiverId)
        }?.id
        
        // Update the messages flow for this chat session
        if (chatSessionId != null) {
            val currentMessages = messagesFlowMap[chatSessionId]?.value ?: emptyList()
            messagesFlowMap[chatSessionId]?.value = (currentMessages + message)
                .sortedBy { it.timestamp }
            
            // Update the chat session with the newest message
            val sessionIndex = chatSessions.indexOfFirst { it.id == chatSessionId }
            if (sessionIndex != -1) {
                val session = chatSessions[sessionIndex]
                chatSessions[sessionIndex] = session.copy(
                    lastMessage = message,
                    unreadCount = 0 // Reset unread count for sent messages
                )
                
                // Update chat sessions flow
                chatSessionsFlow.value = chatSessions.toList()
            }
        }
        
        // Simulate success
        return Result.success(message)
    }

    /**
     * Updates the status of a message identified by its UUID.
     */
    override suspend fun updateMessageStatus(messageId: UUID, status: MessageStatus) {
        // Find and update the message
        val messageIndex = allMessages.indexOfFirst { it.id == messageId }
        if (messageIndex != -1) {
            val message = allMessages[messageIndex]
            val updatedMessage = message.copy(status = status)
            allMessages[messageIndex] = updatedMessage
            
            // Update in all flows
            chatSessions.forEach { session ->
                if (session.lastMessage?.id == messageId) {
                    // Update the last message in the session
                    val sessionIndex = chatSessions.indexOf(session)
                    chatSessions[sessionIndex] = session.copy(lastMessage = updatedMessage)
                }
                
                // Update in message flows
                messagesFlowMap[session.id]?.let { flow ->
                    val messages = flow.value.toMutableList()
                    val index = messages.indexOfFirst { it.id == messageId }
                    if (index != -1) {
                        messages[index] = updatedMessage
                        flow.value = messages
                    }
                }
            }
            
            // Update chat sessions flow
            chatSessionsFlow.value = chatSessions.toList()
        }
    }

    /**
     * Deletes the message identified by the given UUID.
     */
    override suspend fun deleteMessage(messageId: UUID) {
        // Remove the message from all messages
        val removedMessage = allMessages.find { it.id == messageId }
        allMessages.removeIf { it.id == messageId }
        
        // Remove from flows
        if (removedMessage != null) {
            chatSessions.forEach { session ->
                // Update last message if needed
                if (session.lastMessage?.id == messageId) {
                    val sessionIndex = chatSessions.indexOf(session)
                    // Find the new last message for this session
                    val newLastMessage = allMessages
                        .filter { msg -> 
                            session.participantIds.contains(msg.senderId) && 
                            session.participantIds.contains(msg.receiverId)
                        }
                        .maxByOrNull { it.timestamp }
                    
                    chatSessions[sessionIndex] = session.copy(lastMessage = newLastMessage)
                }
                
                // Update message flows
                messagesFlowMap[session.id]?.let { flow ->
                    flow.value = flow.value.filter { it.id != messageId }
                }
            }
            
            // Update chat sessions flow
            chatSessionsFlow.value = chatSessions.toList()
        }
    }

    /**
     * Returns a flow that emits lists of all chat sessions.
     */
    override suspend fun getChatSessions(): Flow<List<ChatSession>> = chatSessionsFlow

    /**
     * Manually simulate receiving a new message from a contact.
     * This can be used to test the UI response to new incoming messages.
     */
    fun simulateIncomingMessage(contactId: UUID, content: String) {
        // Find the chat session for this contact
        val chatSession = chatSessions.find { session ->
            session.participantIds.contains(contactId) &&
            session.participantIds.contains(currentUserId)
        }
        
        if (chatSession != null) {
            // Create a new message
            val newMessage = Message(
                id = UUID.randomUUID(),
                content = content,
                timestamp = System.currentTimeMillis(),
                senderId = contactId,
                receiverId = currentUserId,
                status = MessageStatus.DELIVERED,
                type = tech.ziasvannes.safechat.data.models.MessageType.Text,
                encryptedContent = ByteArray(32) { it.toByte() },
                iv = ByteArray(12) { it.toByte() }
            )
            
            // Add to all messages
            allMessages.add(newMessage)
            
            // Update message flow
            messagesFlowMap[chatSession.id]?.let { flow ->
                flow.value = (flow.value + newMessage).sortedBy { it.timestamp }
            }
            
            // Update chat session
            val sessionIndex = chatSessions.indexOf(chatSession)
            chatSessions[sessionIndex] = chatSession.copy(
                lastMessage = newMessage,
                unreadCount = chatSession.unreadCount + 1
            )
            
            // Update chat sessions flow
            chatSessionsFlow.value = chatSessions.toList()
        }
    }
}