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

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Returns either the real or test implementation of ContactRepository based on the current test mode.
     *
     * If `TestMode.useTestRepositories` is true, the test repository is provided; otherwise, the real repository is used.
     *
     * @return The selected ContactRepository implementation.
     */
    @Provides
    @Singleton
    fun provideContactRepository(impl: ContactRepositoryImpl): ContactRepository = impl

    @Provides
    @Singleton
    fun provideMessageRepository(impl: MessageRepositoryImpl): MessageRepository = impl

    @Provides
    @Singleton
    fun provideEncryptionRepository(impl: EncryptionRepositoryImpl): EncryptionRepository = impl

    @Provides
    @Singleton
    fun provideAuthRepository(
            apiService: tech.ziasvannes.safechat.data.remote.ApiService
    ): AuthRepository = AuthRepository(apiService)
}
