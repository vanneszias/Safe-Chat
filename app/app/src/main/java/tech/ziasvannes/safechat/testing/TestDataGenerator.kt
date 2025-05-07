package tech.ziasvannes.safechat.testing

import java.util.UUID
import kotlin.random.Random
import tech.ziasvannes.safechat.data.models.ChatSession
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.data.models.ContactStatus
import tech.ziasvannes.safechat.data.models.EncryptionStatus
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.data.models.MessageType

/**
 * Utility class that generates test data for the Safe Chat app.
 *
 * This class provides methods for creating realistic test contacts, messages, and chat sessions for
 * UI testing and development.
 */
object TestDataGenerator {

        // List of sample names for generating contacts
        private val firstNames =
                listOf(
                        "Emma",
                        "Liam",
                        "Olivia",
                        "Noah",
                        "Ava",
                        "Elijah",
                        "Sophia",
                        "William",
                        "Isabella",
                        "James",
                        "Charlotte",
                        "Benjamin",
                        "Amelia",
                        "Lucas",
                        "Mia",
                        "Mason",
                        "Harper",
                        "Ethan",
                        "Evelyn",
                        "Alexander",
                        "Abigail",
                        "Michael",
                        "Emily",
                        "Daniel",
                        "Elizabeth",
                        "Matthew",
                        "Sofia",
                        "Henry",
                        "Madison",
                        "Joseph",
                        "Scarlett",
                        "Jackson"
                )

        private val lastNames =
                listOf(
                        "Smith",
                        "Johnson",
                        "Williams",
                        "Brown",
                        "Jones",
                        "Miller",
                        "Davis",
                        "Garcia",
                        "Rodriguez",
                        "Wilson",
                        "Martinez",
                        "Anderson",
                        "Taylor",
                        "Thomas",
                        "Hernandez",
                        "Moore",
                        "Martin",
                        "Jackson",
                        "Thompson",
                        "White",
                        "Lopez",
                        "Lee",
                        "Gonzalez",
                        "Harris",
                        "Clark",
                        "Lewis",
                        "Robinson",
                        "Walker",
                        "Perez",
                        "Hall",
                        "Young",
                        "Allen",
                        "Sanchez",
                        "Wright",
                        "King",
                        "Scott",
                        "Green",
                        "Baker",
                        "Adams",
                        "Nelson"
                )

        // Sample message content snippets for realistic conversations
        private val messageContentSnippets =
                listOf(
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

        /**
         * Generates a list of sample contacts with randomized names, IDs, public keys, last seen
         * timestamps, statuses, and avatar URLs.
         *
         * @param count The number of contacts to generate. Defaults to 15.
         * @return A list of generated Contact objects for testing purposes.
         */
        fun generateContacts(count: Int = 15): List<Contact> {
                val contacts = mutableListOf<Contact>()

                for (i in 0 until count) {
                        val firstName = firstNames.random()
                        val lastName = lastNames.random()

                        contacts.add(
                                Contact(
                                        id = UUID.randomUUID(),
                                        name = "$firstName $lastName",
                                        publicKey =
                                                "MIIBCgKCAQEA${UUID.randomUUID().toString().replace("-", "")}",
                                        lastSeen =
                                                System.currentTimeMillis() -
                                                        Random.nextLong(
                                                                0,
                                                                86400000 * 7
                                                        ), // Random time in the last week
                                        status = ContactStatus.values().random(),
                                        avatarUrl =
                                                "https://i.pravatar.cc/150?u=${UUID.randomUUID()}" // Random avatar from pravatar.cc
                                )
                        )
                }

                return contacts
        }

        /**
         * Generates a list of randomized messages for a chat session between the current user and a
         * contact.
         *
         * Each message alternates between being sent by the current user and the contact, with
         * randomized message statuses and types (text, image, or file). Message content is selected
         * from predefined snippets, and timestamps are incremented to simulate a realistic
         * conversation flow. All messages use dummy encrypted content and IVs for testing purposes.
         *
         * @param chatSessionId The unique identifier of the chat session.
         * @param currentUserId The unique identifier of the current user.
         * @param contactId The unique identifier of the contact.
         * @param count The number of messages to generate (default is 100).
         * @return A list of generated messages sorted by timestamp.
         */
        fun generateMessages(
                chatSessionId: UUID,
                currentUserId: UUID,
                contactId: UUID,
                count: Int = 100
        ): List<Message> {
                val messages = ArrayList<Message>(count) // Preallocate for speed
                val now = System.currentTimeMillis()
                var timestamp = now - (86400000 * 3)
                val dummyIv = ByteArray(12) { it.toByte() }
                val dummyEncryptedContent = ByteArray(32) { it.toByte() }
                val statusSent =
                        listOf(
                                MessageStatus.SENT,
                                MessageStatus.DELIVERED,
                                MessageStatus.READ,
                                MessageStatus.SENDING
                        )
                val snippets = messageContentSnippets
                val random = Random.Default
                for (i in 0 until count) {
                        val isSent = i % 2 == 0
                        val senderId = if (isSent) currentUserId else contactId
                        val receiverId = if (isSent) contactId else currentUserId
                        val status =
                                if (isSent) statusSent[random.nextInt(statusSent.size)]
                                else MessageStatus.READ
                        val messageType =
                                when {
                                        random.nextInt(10) == 0 ->
                                                MessageType.Image(
                                                        "https://picsum.photos/500/300?random=${UUID.randomUUID()}"
                                                )
                                        random.nextInt(20) == 0 ->
                                                MessageType.File(
                                                        url =
                                                                "https://example.com/files/document-${UUID.randomUUID()}.pdf",
                                                        name =
                                                                "document-${random.nextInt(1000)}.pdf",
                                                        size = random.nextLong(1024, 10485760)
                                                )
                                        else -> MessageType.Text
                                }
                        val content = snippets[random.nextInt(snippets.size)]
                        timestamp += random.nextLong(60000, 3600000)
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
                                        iv = dummyIv,
                                        hmac = ByteArray(16) { it.toByte() }
                                )
                        )
                }
                return messages.sortedBy { it.timestamp }
        }

        /**
         * Generates a list of chat sessions for the current user with the provided contacts.
         *
         * For each contact, creates a chat session containing a random number of messages, assigns
         * a random encryption status, and sets the unread message count to zero or a small random
         * value. The last message in each session is determined by the latest timestamp.
         *
         * @param currentUserId The UUID of the current user.
         * @param contacts The list of contacts to generate chat sessions with.
         * @return A list of generated chat sessions, each with randomized messages and attributes.
         */
        fun generateChatSessions(currentUserId: UUID, contacts: List<Contact>): List<ChatSession> {
                return contacts.map { contact ->
                        val messages =
                                generateMessages(
                                        chatSessionId = contact.id,
                                        currentUserId = currentUserId,
                                        contactId = contact.id,
                                        count = Random.nextInt(50, 150)
                                )

                        ChatSession(
                                id = contact.id,
                                participantIds = listOf(currentUserId, contact.id),
                                lastMessage = messages.maxByOrNull { it.timestamp },
                                unreadCount =
                                        if (Random.nextInt(5) == 0) Random.nextInt(1, 10)
                                        else 0, // Some chats have unread messages
                                encryptionStatus =
                                        EncryptionStatus.values()
                                                .random() // Random encryption status
                        )
                }
        }

        // Current user data
        val currentUserId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val currentUserName = "Me"
}
