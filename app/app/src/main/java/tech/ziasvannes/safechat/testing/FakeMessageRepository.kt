package tech.ziasvannes.safechat.testing

import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tech.ziasvannes.safechat.data.models.ChatSession
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.MessageRepository

/**
 * A fake implementation of MessageRepository that provides test data for UI testing and
 * development.
 *
 * This repository generates realistic test messages and chat sessions without requiring a
 * connection to an actual backend API.
 */
class FakeMessageRepository @Inject constructor(private val contactRepository: ContactRepository) :
        MessageRepository {

    // Use the currentUserId from TestDataGenerator for consistency
    val currentUserId = TestDataGenerator.currentUserId

    // All messages across all chats
    private val allMessages = mutableListOf<Message>()

    // Chat sessions for conversations
    val chatSessions = mutableListOf<ChatSession>()

    // Observable flows
    private val chatSessionsFlow = MutableStateFlow<List<ChatSession>>(emptyList())
    private val messagesFlowMap = mutableMapOf<UUID, MutableStateFlow<List<Message>>>()

    init {
        // Initialize with test data asynchronously
        initializeTestData()
    }

    /**
     * Initializes test data for contacts, chat sessions, and messages.
     *
     * Populates the repository with generated contacts, chat sessions, and messages for testing
     * purposes. Sets up message flows for each chat session and updates the chat sessions flow with
     * the generated data.
     */
    private fun initializeTestData() {
        // Get contacts - we'll do this synchronously for the fake implementation
        val contacts =
                runCatching {
                            val contactsField =
                                    (contactRepository as? FakeContactRepository)?.javaClass
                                            ?.getDeclaredField("contacts")
                            contactsField?.isAccessible = true
                            contactsField?.get(contactRepository) as? MutableList<*>
                        }
                        .getOrNull()
                        ?.filterIsInstance<tech.ziasvannes.safechat.data.models.Contact>()
                        ?: TestDataGenerator.generateContacts()

        // Generate chat sessions and messages for each contact
        chatSessions.addAll(TestDataGenerator.generateChatSessions(currentUserId, contacts))

        // Populate allMessages with messages from all chat sessions
        chatSessions.forEach { session ->
            val contact =
                    contacts.find { it.id in session.participantIds && it.id != currentUserId }
            if (contact != null) {
                val messages =
                        TestDataGenerator.generateMessages(
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
     * Returns a flow that emits the list of messages for the given chat session.
     *
     * If no flow exists for the specified session, it is created from the current in-memory
     * messages.
     *
     * @param chatSessionId The UUID of the chat session whose messages are to be observed.
     * @return A [Flow] emitting the list of messages for the specified chat session, ordered by
     * timestamp.
     */
    override suspend fun getMessages(chatSessionId: UUID): Flow<List<Message>> {
        // Create the flow if it doesn't exist
        if (!messagesFlowMap.containsKey(chatSessionId)) {
            messagesFlowMap[chatSessionId] =
                    MutableStateFlow(
                            allMessages
                                    .filter { message ->
                                        val session = chatSessions.find { it.id == chatSessionId }
                                        session?.participantIds?.contains(message.senderId) ==
                                                true &&
                                                session.participantIds.contains(message.receiverId)
                                    }
                                    .sortedBy { it.timestamp }
                    )
        }

        return messagesFlowMap[chatSessionId] ?: MutableStateFlow(emptyList())
    }

    /**
     * Adds a new message to the in-memory store, updates relevant chat session metadata, and
     * returns the sent message as a successful result.
     *
     * The message is appended to the appropriate chat session's message flow, the session's last
     * message is updated, and the unread count is reset.
     *
     * @return A [Result] containing the sent [Message].
     */
    override suspend fun sendMessage(message: Message): Result<Message> {
        // Add message to appropriate lists
        allMessages.add(message)

        // Find the chat session for this message
        val chatSessionId =
                chatSessions
                        .find { session ->
                            session.participantIds.contains(message.senderId) &&
                                    session.participantIds.contains(message.receiverId)
                        }
                        ?.id

        // Update the messages flow for this chat session
        if (chatSessionId != null) {
            val currentMessages = messagesFlowMap[chatSessionId]?.value ?: emptyList()
            messagesFlowMap[chatSessionId]?.value =
                    (currentMessages + message).sortedBy { it.timestamp }

            // Update the chat session with the newest message
            val sessionIndex = chatSessions.indexOfFirst { it.id == chatSessionId }
            if (sessionIndex != -1) {
                val session = chatSessions[sessionIndex]
                chatSessions[sessionIndex] =
                        session.copy(
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
     * Updates the status of a message with the specified UUID and propagates the change to all
     * relevant message flows and chat sessions.
     *
     * @param messageId The UUID of the message to update.
     * @param status The new status to assign to the message.
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
     * Removes a message by its UUID from all internal collections and updates related chat sessions
     * and message flows.
     *
     * If the deleted message was the last message in any chat session, updates the session's last
     * message accordingly.
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
                    val newLastMessage =
                            allMessages
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
     * Returns a flow emitting the current list of all chat sessions.
     *
     * The flow updates whenever chat sessions are added, removed, or modified.
     *
     * @return A [Flow] that emits the list of [ChatSession]s.
     */
    override suspend fun getChatSessions(): Flow<List<ChatSession>> = chatSessionsFlow

    /**
     * Simulates the receipt of a new incoming message from the specified contact.
     *
     * Creates and adds a new message to the appropriate chat session, updates message and chat
     * session flows, and increments the unread count for the session. Intended for testing UI
     * behavior in response to incoming messages.
     *
     * @param contactId The UUID of the contact sending the simulated message.
     * @param content The content of the simulated incoming message.
     */
    fun simulateIncomingMessage(contactId: UUID, content: String) {
        // Find the chat session for this contact
        val chatSession =
                chatSessions.find { session ->
                    session.participantIds.contains(contactId) &&
                            session.participantIds.contains(currentUserId)
                }

        if (chatSession != null) {
            // Create a new message
            val newMessage =
                    Message(
                            id = UUID.randomUUID(),
                            content = content,
                            timestamp = System.currentTimeMillis(),
                            senderId = contactId,
                            receiverId = currentUserId,
                            status = MessageStatus.DELIVERED,
                            type = tech.ziasvannes.safechat.data.models.MessageType.Text,
                            encryptedContent = ByteArray(32) { it.toByte() },
                            iv = ByteArray(12) { it.toByte() },
                            hmac = ByteArray(16) { it.toByte() }
                    )

            // Add to all messages
            allMessages.add(newMessage)

            // Update message flow
            messagesFlowMap[chatSession.id]?.let { flow ->
                flow.value = (flow.value + newMessage).sortedBy { it.timestamp }
            }

            // Update chat session
            val sessionIndex = chatSessions.indexOf(chatSession)
            chatSessions[sessionIndex] =
                    chatSession.copy(
                            lastMessage = newMessage,
                            unreadCount = chatSession.unreadCount + 1
                    )

            // Update chat sessions flow
            chatSessionsFlow.value = chatSessions.toList()
        }
    }

    override suspend fun getOrCreateChatSessionForContact(contactId: UUID): ChatSession {
        // Ensure the contact exists in the repository
        val contactExists =
                (contactRepository as? FakeContactRepository)?.let { repo ->
                    repo.getContactById(contactId) != null
                }
                        ?: true // If not fake, assume exists
        if (!contactExists) {
            // Add a placeholder contact
            contactRepository?.addContact(
                    tech.ziasvannes.safechat.data.models.Contact(
                            id = contactId,
                            name = "New Contact",
                            publicKey = "MIIBCgKCAQEA_PLACEHOLDER_KEY",
                            lastSeen = System.currentTimeMillis(),
                            status = tech.ziasvannes.safechat.data.models.ContactStatus.OFFLINE,
                            avatarUrl = null
                    )
            )
        }
        // Try to find an existing session
        val existing = chatSessions.find { it.participantIds.contains(contactId) }
        if (existing != null) return existing
        // Create a new session
        val newSession =
                ChatSession(
                        id = contactId,
                        participantIds = listOf(currentUserId, contactId),
                        lastMessage = null,
                        unreadCount = 0,
                        encryptionStatus =
                                tech.ziasvannes.safechat.data.models.EncryptionStatus.ENCRYPTED
                )
        chatSessions.add(newSession)
        messagesFlowMap[contactId] = MutableStateFlow(emptyList())
        chatSessionsFlow.value = chatSessions.toList()
        return newSession
    }
}

object TestMessageSimulator {
    private var job: Job? = null
    fun start(repository: FakeMessageRepository) {
        if (job != null) return
        job =
                CoroutineScope(Dispatchers.Default).launch {
                    while (isActive) {
                        if (TestMode.simulateIncomingMessages) {
                            // For each chat, simulate a message from the contact
                            repository.chatSessions.forEach { session ->
                                val contactId =
                                        session.participantIds.firstOrNull {
                                            it != repository.currentUserId
                                        }
                                if (contactId != null) {
                                    repository.simulateIncomingMessage(
                                            contactId,
                                            "[Simulated incoming message]"
                                    )
                                }
                            }
                        }
                        delay(3000) // Every 3 seconds
                    }
                }
    }
    fun stop() {
        job?.cancel()
        job = null
    }
}
