package tech.ziasvannes.safechat.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import tech.ziasvannes.safechat.data.repository.AuthRepository
import tech.ziasvannes.safechat.data.repository.ContactRepositoryImpl
import tech.ziasvannes.safechat.data.repository.EncryptionRepositoryImpl
import tech.ziasvannes.safechat.data.repository.MessageRepositoryImpl
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideContactRepository(impl: ContactRepositoryImpl): ContactRepository = impl

    @Provides
    @Singleton
    fun provideMessageRepository(impl: MessageRepositoryImpl): MessageRepository = impl

    @Provides
    @Singleton
    fun provideEncryptionRepository(@ApplicationContext context: Context): EncryptionRepository = EncryptionRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideAuthRepository(
            apiService: tech.ziasvannes.safechat.data.remote.ApiService,
            encryptionRepository: EncryptionRepository,
            userSession: tech.ziasvannes.safechat.session.UserSession
    ): AuthRepository = AuthRepository(apiService, encryptionRepository, userSession)
}
