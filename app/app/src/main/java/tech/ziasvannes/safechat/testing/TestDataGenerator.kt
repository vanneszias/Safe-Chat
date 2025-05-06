package tech.ziasvannes.safechat.testing

import tech.ziasvannes.safechat.data.models.ChatSession
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.data.models.ContactStatus
import tech.ziasvannes.safechat.data.models.EncryptionStatus
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.data.models.MessageType
import java.util.UUID
import kotlin.random.Random

/**
 * Utility class that generates test data for the Safe Chat app.
 *
 * This class provides methods for creating realistic test contacts, messages,
 * and chat sessions for UI testing and development.
 */
object TestDataGenerator {

    // List of sample names for generating contacts
    private val firstNames = listOf(
        "Emma", "Liam", "Olivia", "Noah", "Ava", "Elijah", "Sophia", "William",
        "Isabella", "James", "Charlotte", "Benjamin", "Amelia", "Lucas", "Mia", "Mason",
        "Harper", "Ethan", "Evelyn", "Alexander", "Abigail", "Michael", "Emily", "Daniel",
        "Elizabeth", "Matthew", "Sofia", "Henry", "Madison", "Joseph", "Scarlett", "Jackson"
    )

    private val lastNames = listOf(
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Garcia",
        "Rodriguez", "Wilson", "Martinez", "Anderson", "Taylor", "Thomas", "Hernandez",
        "Moore", "Martin", "Jackson", "Thompson", "White", "Lopez", "Lee", "Gonzalez",
        "Harris", "Clark", "Lewis", "Robinson", "Walker", "Perez", "Hall", "Young",
        "Allen", "Sanchez", "Wright", "King", "Scott", "Green", "Baker", "Adams", "Nelson"
    )

    // Sample message content snippets for realistic conversations
    private val messageContentSnippets = listOf(
        "Hey, how are you doing?",
        "Did you see that new movie yet?",
        "I'm heading to the store, need anything?",
        "Can we meet up tomorrow?",
        "Just sent you the files you requested",
        "Happy birthday! 🎉",
        "Remember to bring your laptop to the meeting",
        "Thanks for your help yesterday",
        "Check out this article I found",
        "I think we need to reschedule our call",
        "What time works for you?",
        "Have you tried the new restaurant downtown?",
        "Congratulations on your promotion! 🎊",
        "Did you finish the project?",
        "Can you send me the address?",
        "I'll be there in 10 minutes",
        "Don't forget about the deadline on Friday",
        "How was your weekend?",
        "I just got your message",
        "Let me know when you're free to talk"
    )

    // Generate a list of sample contacts
    fun generateContacts(count: Int = 15): List<Contact> {
        val contacts = mutableListOf<Contact>()
        
        for (i in 0 until count) {
            val firstName = firstNames.random()
            val lastName = lastNames.random()
            
            contacts.add(
                Contact(
                    id = UUID.randomUUID(),
                    name = "$firstName $lastName",
                    publicKey = "MIIBCgKCAQEA${UUID.randomUUID().toString().replace("-", "")}",
                    lastSeen = System.currentTimeMillis() - Random.nextLong(0, 86400000 * 7), // Random time in the last week
                    status = ContactStatus.values().random(),
                    avatarUrl = "https://i.pravatar.cc/150?u=${UUID.randomUUID()}" // Random avatar from pravatar.cc
                )
            )
        }
        
        return contacts
    }
    
    // Generate random messages for a chat session
    fun generateMessages(
        chatSessionId: UUID,
        currentUserId: UUID,
        contactId: UUID,
        count: Int = 20
    ): List<Message> {
        val messages = mutableListOf<Message>()
        val now = System.currentTimeMillis()
        var timestamp = now - (86400000 * 3) // Start from 3 days ago
        
        // Create a small IV and encrypted content for testing
        val dummyIv = ByteArray(12) { it.toByte() }
        val dummyEncryptedContent = ByteArray(32) { it.toByte() }
        
        for (i in 0 until count) {
            // Alternate between sent and received messages
            val isSent = i % 2 == 0
            val senderId = if (isSent) currentUserId else contactId
            val receiverId = if (isSent) contactId else currentUserId
            
            // Randomize message status based on whether it's sent or received
            val status = when {
                isSent -> listOf(
                    MessageStatus.SENT,
                    MessageStatus.DELIVERED,
                    MessageStatus.READ,
                    MessageStatus.SENDING
                ).random()
                else -> MessageStatus.READ // Received messages are always read in this test data
            }
            
            // Occasionally add image or file messages
            val messageType = when {
                Random.nextInt(10) == 0 -> MessageType.Image("https://picsum.photos/500/300?random=${UUID.randomUUID()}")
                Random.nextInt(20) == 0 -> MessageType.File(
                    url = "https://example.com/files/document-${UUID.randomUUID()}.pdf",
                    name = "document-${Random.nextInt(1000)}.pdf",
                    size = Random.nextLong(1024, 10485760) // Between 1KB and 10MB
                )
                else -> MessageType.Text
            }
            
            // Use realistic message content
            val content = messageContentSnippets.random()
            
            // Increment timestamp to get sequential ordering
            timestamp += Random.nextLong(60000, 3600000) // Add between 1 minute and 1 hour
            
            messages.add(
                Message(
                    id = UUID.randomUUID(),
                    content = content,
                    timestamp = timestamp,
                    senderId = senderId,
                    receiverId = receiverId,
                    status = status,
                    type = messageType,
                    encryptedContent = dummyEncryptedContent,
                    iv = dummyIv
                )
            )
        }
        
        // Sort by timestamp
        return messages.sortedBy { it.timestamp }
    }
    
    // Generate chat sessions from contacts
    fun generateChatSessions(
        currentUserId: UUID,
        contacts: List<Contact>
    ): List<ChatSession> {
        return contacts.map { contact ->
            val messages = generateMessages(
                chatSessionId = UUID.randomUUID(),
                currentUserId = currentUserId,
                contactId = contact.id,
                count = Random.nextInt(5, 30) // Random number of messages per chat
            )
            
            ChatSession(
                id = UUID.randomUUID(),
                participantIds = listOf(currentUserId, contact.id),
                lastMessage = messages.maxByOrNull { it.timestamp },
                unreadCount = if (Random.nextInt(5) == 0) Random.nextInt(1, 10) else 0, // Some chats have unread messages
                encryptionStatus = EncryptionStatus.values().random() // Random encryption status
            )
        }
    }
    
    // Current user data
    val currentUserId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val currentUserName = "Me"
}