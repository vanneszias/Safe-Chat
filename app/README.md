# Safe Chat - Secure Messaging Android Application

## Project Overview

Safe Chat is a secure messaging Android application built with Kotlin, implementing end-to-end encryption using X25519 key exchange and AES-GCM encryption. The app demonstrates modern Android development practices with Clean Architecture, Jetpack Compose, and real-time WebSocket communication.

## Architecture Overview

The application follows **Clean Architecture** principles with **MVVM (Model-View-ViewModel)** pattern, ensuring separation of concerns, testability, and maintainable code.

### Project Structure

```
app/
├── data/
│   ├── local/
│   │   ├── entity/        # Room database entities
│   │   └── dao/           # Data Access Objects
│   ├── models/            # Domain data models
│   ├── remote/            # API service and DTOs
│   ├── repository/        # Repository implementations
│   └── websocket/         # WebSocket management
├── domain/
│   ├── repository/        # Repository interfaces
│   └── usecases/          # Business logic use cases
├── presentation/
│   ├── navigation/        # Navigation components
│   ├── screens/           # Composable screens
│   ├── viewmodels/        # ViewModels for each screen
│   └── components/        # Reusable UI components
├── di/                    # Dependency Injection (Hilt)
├── session/               # User session management
└── ui/
    └── theme/             # Material Design theme
```

## Current Implementation Status

✅ **Completed Features:**
- User registration and authentication with secure credential storage
- Real-time messaging with WebSocket connections
- End-to-end encryption using X25519 + AES-GCM
- Contact management with public key exchange
- Message status tracking (SENDING → SENT → READ)
- Local message storage with Room database
- Modern UI with Jetpack Compose and Material Design 3
- Secure key storage using Android Keystore and EncryptedSharedPreferences
- Profile management with avatar support
- Connection management with automatic reconnection
- Bidirectional status updates for message synchronization

## Technology Stack

### Core Technologies
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose with Material Design 3
- **Architecture:** Clean Architecture + MVVM
- **Dependency Injection:** Hilt
- **Database:** Room (SQLite) for local storage
- **Networking:** Retrofit for REST API, OkHttp for WebSocket
- **Reactive Programming:** Kotlin Coroutines and Flow

### Security & Cryptography
- **Key Exchange:** X25519 (Elliptic Curve Diffie-Hellman)
- **Message Encryption:** AES-GCM with secure random IVs
- **Key Storage:** Android Keystore for private keys
- **Credential Storage:** EncryptedSharedPreferences
- **Cryptographic Library:** Bouncy Castle + Google Tink
- **JWT:** Secure token storage and management

### Additional Libraries
- **Image Loading:** Coil for profile avatars
- **Serialization:** Gson for JSON parsing
- **Security:** AndroidX Security Crypto for encrypted preferences

## Data Models

### Core Domain Models

#### Contact
```kotlin
data class Contact(
    val id: UUID,
    val name: String,
    val publicKey: String,
    val lastSeen: Long,
    val status: ContactStatus,
    val avatar: String? = null
)
```

#### Message
```kotlin
data class Message(
    val id: UUID,
    val content: String?,
    val timestamp: Long,
    val senderId: UUID,
    val receiverId: UUID,
    val status: MessageStatus,
    val type: MessageType,
    val encryptedContent: ByteArray,
    val iv: ByteArray
)
```

#### Message Status Flow
```kotlin
enum class MessageStatus {
    SENDING,    // Message created locally
    SENT,       // Confirmed received by server
    READ,       // Read by recipient
    FAILED      // Failed to send
}
```

## Security Implementation

### Encryption Architecture
1. **Key Generation:** X25519 key pairs generated on device
2. **Shared Secret:** ECDH key exchange for each contact
3. **Message Encryption:** AES-GCM with unique IVs per message
4. **Key Storage:** Private keys secured in Android Keystore
5. **Forward Secrecy:** New shared secrets for enhanced security

### Security Features
- **End-to-End Encryption:** Messages encrypted before leaving device
- **Secure Key Storage:** Hardware-backed Android Keystore when available
- **No Server Plaintext:** Server never sees unencrypted message content
- **Automatic Cleanup:** Messages deleted from server after read confirmation
- **Secure Authentication:** JWT tokens with secure storage
- **Input Validation:** Comprehensive validation throughout the app

## Repository Pattern Implementation

### Local Data Management
- **ContactRepositoryImpl:** Manages local contact storage and synchronization
- **LocalMessageRepositoryImpl:** Handles local message database operations
- **EncryptionRepositoryImpl:** Manages cryptographic operations and key storage

### Remote Data Management
- **AuthRepository:** Handles user authentication and profile management
- **WebSocketMessageRepositoryImpl:** Manages real-time messaging and status updates

### Data Synchronization
- Single source of truth: Local Room database
- WebSocket events directly update local storage
- Reactive UI updates through Flow emissions

## UI Architecture (Jetpack Compose)

### Screen Composition
- **ContactListScreen:** Displays contacts with real-time status
- **ChatDetailScreen:** Message thread with encryption indicators
- **AddContactScreen:** Public key-based contact addition
- **ProfileScreen:** User profile and settings management
- **AuthScreens:** Login and registration flows

### Navigation
- **Bottom Navigation:** Main app sections (Chats, Profile, Settings)
- **Nested Navigation:** Deep linking support for chat threads
- **State Management:** ViewModel-based state handling with proper lifecycle awareness

### Theme System
- **Material Design 3:** Modern design language
- **Dynamic Colors:** System-aware theming
- **Dark Mode:** Full dark theme support
- **Responsive Design:** Adaptive layouts for different screen sizes

## WebSocket Implementation

### Real-time Communication
- **Connection Management:** Automatic connection with reconnection logic
- **Message Broadcasting:** Instant message delivery
- **Status Synchronization:** Real-time status updates for both parties
- **Presence Indicators:** Online/offline status for contacts

### Event Handling
```kotlin
sealed class WebSocketEvent {
    data class NewMessage(val message: MessageNotification) : WebSocketEvent()
    data class StatusUpdate(val update: StatusUpdateData) : WebSocketEvent()
    data class UserOnline(val user: UserStatus) : WebSocketEvent()
    data class UserOffline(val user: UserStatus) : WebSocketEvent()
    object Connected : WebSocketEvent()
    object Disconnected : WebSocketEvent()
    data class Error(val error: String) : WebSocketEvent()
}
```

## Development Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Kotlin 1.9.0+
- Android SDK 24+ (minimum), targeting SDK 35
- Java 11

### Build Configuration
```kotlin
android {
    compileSdk = 35
    defaultConfig {
        minSdk = 24
        targetSdk = 35
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
```

### Key Dependencies
```gradle
// Core Android
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

// Compose
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.activity:activity-compose:1.8.2")

// Architecture
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.navigation:navigation-compose:2.7.6")

// Dependency Injection
implementation("com.google.dagger:hilt-android:2.48")
kapt("com.google.dagger:hilt-compiler:2.48")

// Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// Security
implementation("com.google.crypto.tink:tink-android:1.10.0")
implementation("org.bouncycastle:bcprov-jdk18on:1.77")
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

## Testing Strategy

### Unit Testing
- **Repository Tests:** Data layer testing with mock dependencies
- **ViewModel Tests:** Business logic validation
- **Encryption Tests:** Cryptographic operation verification
- **Utility Tests:** Helper function validation

### Integration Testing
- **Database Tests:** Room database operations
- **API Tests:** Network layer integration
- **WebSocket Tests:** Real-time communication testing

### UI Testing
- **Compose Tests:** Screen composition and interaction testing
- **Navigation Tests:** Screen flow validation
- **Accessibility Tests:** Screen reader compatibility

## Security Considerations

### Production Readiness
⚠️ **This is a proof-of-concept implementation for educational purposes.**

For production use, consider:
- **Key Rotation:** Implement periodic key rotation mechanisms
- **Certificate Pinning:** Pin server certificates for API communication
- **Root Detection:** Implement root/jailbreak detection
- **Code Obfuscation:** Protect against reverse engineering
- **Secure Communication:** Implement additional transport security
- **Audit & Compliance:** Regular security audits and compliance validation

### Privacy Features
- **Local Data Encryption:** All local data encrypted at rest
- **Memory Protection:** Sensitive data cleared from memory
- **Secure Deletion:** Cryptographic key deletion when appropriate
- **Minimal Data Collection:** No unnecessary personal data storage

## Build Instructions

1. **Clone Repository:**
   ```bash
   git clone <repository-url>
   cd Safe-Chat/app
   ```

2. **Backend Setup:**
   ```bash
   # Ensure backend is running on localhost:8080
   cd ../backend
   cargo run
   ```

3. **Android Build:**
   ```bash
   # Open in Android Studio or use command line
   ./gradlew build
   ./gradlew installDebug
   ```

4. **Environment Configuration:**
   - Update API base URL in `ApiService.kt` if needed
   - Configure WebSocket endpoint for your backend

## Future Enhancements

### Planned Features
- **Group Messaging:** Multi-participant encrypted conversations
- **File Sharing:** Encrypted file and image transmission
- **Voice Messages:** Encrypted audio message support
- **Message Search:** Local encrypted message search
- **Backup/Restore:** Secure key and message backup system
- **Push Notifications:** Encrypted push notification support

### Performance Optimizations
- **Message Pagination:** Efficient loading of large conversation histories
- **Image Optimization:** Compressed image transmission and caching
- **Background Sync:** Efficient background message synchronization
- **Memory Management:** Optimized memory usage for large conversations

---

**This Android application works seamlessly with the Safe Chat Rust backend, providing a complete secure messaging solution with modern Android development practices and robust end-to-end encryption.**