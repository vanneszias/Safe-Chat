# Safe Chat - Secure Messaging Application

## Project Overview

Safe Chat is a secure messaging application built with Kotlin Native for Android, implementing end-to-end encryption using the Diffie-Hellman key exchange protocol.

## Architecture Overview

The application follows MVVM (Model-View-ViewModel) architecture with Clean Architecture principles, ensuring separation of concerns and maintainable code.

### Project Structure

```
app/
├── data/
│   ├── models/          # Data entities
│   ├── repositories/    # Repository implementations
│   └── datasources/     # Local and remote data sources
├── domain/
│   ├── usecases/       # Business logic
│   └── repositories/    # Repository interfaces
├── presentation/
│   ├── viewmodels/     # ViewModels for each screen
│   ├── views/          # Composable screens
│   └── components/     # Reusable UI components
└── utils/
    ├── encryption/     # Encryption utilities
    └── extensions/     # Kotlin extensions
```

## Development Stages

### Stage 1: Core Architecture Setup

#### 1.1 Data Layer Implementation

##### Models

```kotlin
data class Contact(
    val id: UUID,                    // Unique identifier for the contact
    val name: String,                // Display name of the contact
    val publicKey: String,           // Public key used for encryption
    val lastSeen: Long,              // Timestamp of last activity
    val status: ContactStatus,       // Current online status
    val avatar: String?              // Optional profile picture URL
)

data class Message(
    val id: UUID,                    // Unique identifier for the message
    val content: String,             // Decrypted message content
    val timestamp: Long,             // Time when message was sent
    val senderId: UUID,              // ID of message sender
    val receiverId: UUID,            // ID of message recipient
    val status: MessageStatus,       // Current delivery status
    val type: MessageType,           // Type of message content
    val encryptedContent: ByteArray, // Encrypted message content
    val iv: ByteArray                // Initialization vector for encryption
)

data class ChatSession(
    val id: UUID,                    // Unique identifier for the chat
    val participantIds: List<UUID>,  // List of users in the chat
    val lastMessage: Message?,       // Most recent message
    val unreadCount: Int,            // Number of unread messages
    val encryptionStatus: EncryptionStatus // Current encryption state
)

sealed class MessageType {
    object Text : MessageType()                                                    // Plain text messages
    data class Image(val url: String) : MessageType()                             // Image messages with URL
    data class File(val url: String, val name: String, val size: Long) : MessageType() // File attachments
}

enum class MessageStatus {
    SENDING,    // Message is being sent
    SENT,       // Message has been sent to server
    DELIVERED,  // Message has been delivered to recipient
    READ,       // Message has been read by recipient
    FAILED      // Message failed to send
}

enum class ContactStatus {
    ONLINE,     // Contact is currently active
    OFFLINE,    // Contact is not connected
    AWAY        // Contact is inactive
}

enum class EncryptionStatus {
    NOT_ENCRYPTED,           // No encryption established
    KEY_EXCHANGE_IN_PROGRESS, // Setting up encryption
    ENCRYPTED               // Secure communication active
}
```

##### Repository Interfaces

```kotlin
interface ContactRepository {
    suspend fun getContacts(): Flow<List<Contact>>                    // Retrieves list of all contacts
    suspend fun addContact(contact: Contact)                         // Adds a new contact to the repository
    suspend fun updateContact(contact: Contact)                      // Updates existing contact information
    suspend fun deleteContact(contactId: UUID)                       // Removes a contact from the repository
    suspend fun getContactById(id: UUID): Contact?                   // Finds a specific contact by ID
    suspend fun searchContacts(query: String): Flow<List<Contact>>   // Searches contacts based on query string
}

interface MessageRepository {
    suspend fun getMessages(chatSessionId: UUID): Flow<List<Message>>  // Retrieves messages for a specific chat
    suspend fun sendMessage(message: Message): Result<Message>         // Sends a new message and returns result
    suspend fun updateMessageStatus(messageId: UUID, status: MessageStatus)  // Updates message delivery status
    suspend fun deleteMessage(messageId: UUID)                         // Deletes a specific message
    suspend fun getChatSessions(): Flow<List<ChatSession>>            // Retrieves all active chat sessions
}

interface EncryptionRepository {
    suspend fun generateKeyPair(): KeyPair                            // Creates new encryption key pair
    suspend fun storeKeyPair(keyPair: KeyPair)                       // Saves key pair securely
    suspend fun getPublicKey(): PublicKey                            // Retrieves stored public key
    suspend fun getPrivateKey(): PrivateKey                          // Retrieves stored private key
    suspend fun computeSharedSecret(publicKey: PublicKey): ByteArray  // Calculates shared secret for encryption
}
```

#### 1.2 Domain Layer Implementation

##### Use Cases

```kotlin
class GetContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository                   // Repository dependency for contact operations
) {
    operator fun invoke(): Flow<List<Contact>> =                      // Returns flow of contacts list
        contactRepository.getContacts()
}

class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,                  // Repository for message operations
    private val encryptionRepository: EncryptionRepository           // Repository for encryption operations
) {
    suspend operator fun invoke(
        content: String,                                             // Message content to be sent
        receiverId: UUID,                                           // ID of message recipient
        type: MessageType                                           // Type of message (text, image, etc.)
    ): Result<Message> {
        // Implementation details for message encryption and sending
    }
}

class InitiateKeyExchangeUseCase @Inject constructor(
    private val encryptionRepository: EncryptionRepository           // Repository for encryption operations
) {
    suspend operator fun invoke(contactId: UUID): Result<Unit> {     // Initiates key exchange with contact
        // Implementation details for key exchange
    }
}
```

#### 1.3 Presentation Layer Implementation

##### ViewModels

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,               // Use case for sending messages
    private val getMessagesUseCase: GetMessagesUseCase,              // Use case for retrieving messages
    savedStateHandle: SavedStateHandle                               // Saves view state across configurations
) : ViewModel() {
    private val _chatState = MutableStateFlow<ChatState>(ChatState.Loading)  // Mutable state for chat UI
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()           // Immutable state exposed to UI

    // Implementation details
}

data class ChatState(
    val messages: List<Message> = emptyList(),                       // List of messages in chat
    val isLoading: Boolean = false,                                  // Loading state indicator
    val error: String? = null,                                       // Error message if any
    val encryptionStatus: EncryptionStatus = EncryptionStatus.NOT_ENCRYPTED  // Current encryption state
)
```

### Stage 2: UI Implementation

#### 2.1 Theme and Styling

```kotlin
object SafeChatTheme {
    val colors = darkColors(                                         // Dark theme color palette
        primary = Color(0xFF2196F3),                                // Primary brand color
        secondary = Color(0xFF03DAC5),                              // Secondary accent color
        background = Color(0xFF121212),                             // App background color
        surface = Color(0xFF1E1E1E)                                 // Surface elements color
    )

    val typography = Typography(                                     // Text style definitions
        // Typography definitions
    )

    val shapes = Shapes(                                            // UI component shape definitions
        small = RoundedCornerShape(4.dp),                          // Small component corners
        medium = RoundedCornerShape(8.dp),                         // Medium component corners
        large = RoundedCornerShape(12.dp)                          // Large component corners
    )
}
```

#### 2.2 Common Components

- MessageBubble
- ContactListItem
- EncryptionStatusIndicator
- CustomTextField
- LoadingIndicator

#### 2.3 Screen Implementations

- ContactListScreen
- ChatScreen
- ProfileScreen
- SettingsScreen

### Stage 3: Encryption Implementation

#### 3.1 Diffie-Hellman Implementation

```kotlin
class DiffieHellmanManager @Inject constructor(                              // Manager class for Diffie-Hellman key exchange
    private val keyStore: KeyStore,                                          // Secure storage for cryptographic keys
    private val secureRandom: SecureRandom                                   // Cryptographically secure random number generator
) {
    private val keySize = 2048                                              // Size of the encryption key in bits
    private val algorithm = "DH"                                            // Diffie-Hellman algorithm identifier

    fun generateKeyPair(): KeyPair {                                        // Generates public-private key pair
        val parameterSpec = DHParameterSpec(p, g)                          // Parameters for DH key generation
        val keyPairGenerator = KeyPairGenerator.getInstance(algorithm)      // Creates instance of key pair generator
        keyPairGenerator.initialize(parameterSpec)                         // Initializes generator with DH parameters
        return keyPairGenerator.generateKeyPair()                          // Returns the generated key pair
    }

    fun computeSharedSecret(publicKey: PublicKey, privateKey: PrivateKey): ByteArray {  // Computes shared secret key
        val keyAgreement = KeyAgreement.getInstance(algorithm)             // Creates key agreement instance
        keyAgreement.init(privateKey)                                     // Initializes with private key
        keyAgreement.doPhase(publicKey, true)                            // Executes key agreement phase
        return keyAgreement.generateSecret()                              // Returns the computed shared secret
    }

    fun encryptMessage(message: String, sharedSecret: ByteArray): EncryptedMessage {    // Encrypts message using shared secret
        // Implementation details for message encryption
    }

    fun decryptMessage(encryptedMessage: EncryptedMessage, sharedSecret: ByteArray): String {  // Decrypts message using shared secret
        // Implementation details for message decryption
    }
}
```

### Stage 4: API Integration

#### 4.1 API Routes and Models

Base URL: `api.safechat.ziasvannes.tech/v1`

##### Authentication

```http
POST /auth/register
Content-Type: application/json

{
    "username": string,
    "password": string,
    "publicKey": string
}

POST /auth/login
Content-Type: application/json

{
    "username": string,
    "password": string
}

POST /auth/refresh-token
Authorization: Bearer {refresh_token}
```

##### Contacts

```http
GET /contacts
Authorization: Bearer {token}

POST /contacts
Authorization: Bearer {token}
Content-Type: application/json

{
    "username": string,
    "publicKey": string
}

DELETE /contacts/{id}
Authorization: Bearer {token}

PUT /contacts/{id}/key-exchange
Authorization: Bearer {token}
Content-Type: application/json

{
    "publicKey": string
}
```

##### Messages

```http
GET /messages/{chatId}
Authorization: Bearer {token}

POST /messages
Authorization: Bearer {token}
Content-Type: application/json

{
    "receiverId": string,
    "content": string,
    "encryptedContent": string,
    "iv": string,
    "type": string
}

PUT /messages/{id}/status
Authorization: Bearer {token}
Content-Type: application/json

{
    "status": string
}
```

#### 4.2 WebSocket Events

#### 4.2 WebSocket Events

```kotlin
sealed class WebSocketMessage {
    data class NewMessage(
        val messageId: UUID,           // Unique identifier for the message
        val senderId: UUID,            // ID of the user who sent the message
        val encryptedContent: String,  // Encrypted message content
        val iv: String,                // Initialization vector for decryption
        val timestamp: Long            // Time when message was sent
    ) : WebSocketMessage()

    data class MessageStatus(
        val messageId: UUID,           // ID of the message being updated
        val status: MessageStatus,     // New status of the message
        val timestamp: Long            // Time when status was updated
    ) : WebSocketMessage()

    data class ContactStatus(
        val contactId: UUID,           // ID of the contact whose status changed
        val status: ContactStatus,     // New status of the contact
        val lastSeen: Long            // Last active timestamp
    ) : WebSocketMessage()
}

interface WebSocketListener {
    fun onNewMessage(message: WebSocketMessage.NewMessage)
    fun onMessageStatusUpdate(status: WebSocketMessage.MessageStatus)
    fun onContactStatusUpdate(status: WebSocketMessage.ContactStatus)
}
```

### Stage 5: Testing

#### 5.1 Unit Tests

- ViewModel Tests
- UseCase Tests
- Repository Tests
- Encryption Tests

#### 5.2 UI Tests

- Navigation Tests
- Component Tests
- End-to-End Tests

#### 5.3 Security Tests

- Encryption Tests
- Key Exchange Tests
- Storage Security Tests

## Best Practices

### Code Style

- Follow Kotlin coding conventions
- Use meaningful names
- Keep functions small and focused
- Document complex logic
- Use sealed classes for state management

### Architecture

- Single Responsibility Principle
- Dependency Injection (Hilt)
- Repository Pattern
- Clean Architecture
- Immutable State

### Security

- No plaintext storage
- Secure key storage using Android Keystore
- Message authentication using HMAC
- Diffie-Hellman key exchange for secure communication
- Strong encryption algorithms (AES-256)
- Input validation
- Regular security audits
- Perfect Forward Secrecy implementation

### Testing

- TDD approach
- Unit test coverage > 80%
- UI automation tests
- Integration tests
- Security penetration testing

## Dependencies

```kotlin
dependencies {
    // Core Android and Lifecycle components
    implementation(libs.androidx.core.ktx)                              // Kotlin extensions for Android core features
    implementation(libs.androidx.lifecycle.viewmodel.compose)           // ViewModel integration with Compose

    // Jetpack Compose UI components and tooling
    implementation(libs.androidx.compose.ui)                            // Core UI components for Compose
    implementation(libs.androidx.compose.material3)                     // Material Design 3 components
    implementation(libs.androidx.compose.ui.tooling.preview)            // Preview support for Compose UI

    // Navigation components for Compose
    implementation(libs.androidx.navigation.compose)                    // Navigation framework for Compose

    // Dependency injection with Hilt
    implementation(libs.hilt.android)                                  // Hilt for Android dependency injection
    kapt(libs.hilt.compiler)                                          // Annotation processor for Hilt

    // Local database with Room
    implementation(libs.androidx.room.runtime)                         // Room database runtime
    implementation(libs.androidx.room.ktx)                            // Kotlin extensions for Room
    kapt(libs.androidx.room.compiler)                                 // Room annotation processor

    // Network communication
    implementation(libs.retrofit)                                      // HTTP client for API calls
    implementation(libs.retrofit.converter.gson)                       // JSON serialization/deserialization
    implementation(libs.okhttp3.logging.interceptor)                  // Network call logging for debugging

    // Coroutines for asynchronous programming
    implementation(libs.kotlinx.coroutines.android)                    // Android-specific coroutines support

    // Security components
    implementation(libs.androidx.security.crypto)                      // Encrypted data storage
    implementation(libs.tink)                                         // Google's cryptographic library

    // Testing dependencies
    testImplementation(libs.junit)                                    // Unit testing framework
    testImplementation(libs.mockk)                                    // Mocking library for Kotlin
    testImplementation(libs.kotlinx.coroutines.test)                  // Testing utilities for coroutines
    androidTestImplementation(libs.androidx.test.ext.junit)           // Android JUnit extensions
    androidTestImplementation(libs.androidx.test.espresso.core)       // UI testing framework
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)   // Compose UI testing utilities
}
```
