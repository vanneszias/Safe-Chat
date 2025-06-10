package tech.ziasvannes.safechat.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import tech.ziasvannes.safechat.data.local.SafeChatDatabase
import tech.ziasvannes.safechat.data.repository.AuthRepository
import tech.ziasvannes.safechat.data.repository.ContactRepositoryImpl
import tech.ziasvannes.safechat.data.repository.EncryptionRepositoryImpl
import tech.ziasvannes.safechat.data.repository.LocalMessageRepositoryImpl
import tech.ziasvannes.safechat.data.repository.WebSocketMessageRepositoryImpl
import tech.ziasvannes.safechat.data.websocket.WebSocketService
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import tech.ziasvannes.safechat.domain.repository.MessageRepository

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideContactRepository(impl: ContactRepositoryImpl): ContactRepository = impl

    @Provides
    @Singleton
    fun provideBaseMessageRepository(
            db: SafeChatDatabase,
            encryptionRepository: EncryptionRepository,
            contactRepository: ContactRepository,
            userSession: tech.ziasvannes.safechat.session.UserSession,
            apiService: tech.ziasvannes.safechat.data.remote.ApiService
    ): LocalMessageRepositoryImpl =
            LocalMessageRepositoryImpl(
                    messageDao = db.messageDao(),
                    encryptionRepository = encryptionRepository,
                    contactRepository = contactRepository,
                    userSession = userSession,
                    apiService = apiService
            )

    @Provides
    @Singleton
    fun provideMessageRepository(
            localRepository: LocalMessageRepositoryImpl,
            webSocketService: WebSocketService,
            userSession: tech.ziasvannes.safechat.session.UserSession,
            encryptionRepository: EncryptionRepository,
            contactRepository: ContactRepository,
            db: SafeChatDatabase
    ): MessageRepository =
            WebSocketMessageRepositoryImpl(
                    localMessageRepository = localRepository,
                    webSocketService = webSocketService,
                    userSession = userSession,
                    encryptionRepository = encryptionRepository,
                    contactRepository = contactRepository,
                    messageDao = db.messageDao()
            )

    @Provides
    @Singleton
    fun provideEncryptionRepository(@ApplicationContext context: Context): EncryptionRepository =
            EncryptionRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideAuthRepository(
            apiService: tech.ziasvannes.safechat.data.remote.ApiService,
            encryptionRepository: EncryptionRepository,
            userSession: tech.ziasvannes.safechat.session.UserSession
    ): AuthRepository = AuthRepository(apiService, encryptionRepository, userSession)
}
