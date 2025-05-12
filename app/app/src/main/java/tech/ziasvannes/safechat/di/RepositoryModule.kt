package tech.ziasvannes.safechat.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import tech.ziasvannes.safechat.data.repository.ContactRepositoryImpl
import tech.ziasvannes.safechat.data.repository.MessageRepositoryImpl
import tech.ziasvannes.safechat.data.repository.EncryptionRepositoryImpl
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    /**
     * Provides a singleton instance of ContactRepository using ContactRepositoryImpl.
     *
     * @return The ContactRepository implementation.
     */
    @Provides
    @Singleton
    fun provideContactRepository(
        impl: ContactRepositoryImpl
    ): ContactRepository = impl

    /**
     * Provides a singleton instance of MessageRepository using MessageRepositoryImpl.
     *
     * @return The MessageRepository implementation.
     */
    @Provides
    @Singleton
    fun provideMessageRepository(
        impl: MessageRepositoryImpl
    ): MessageRepository = impl

    /**
     * Provides a singleton instance of EncryptionRepository using EncryptionRepositoryImpl.
     *
     * @return The singleton EncryptionRepository implementation.
     */
    @Provides
    @Singleton
    fun provideEncryptionRepository(
        impl: EncryptionRepositoryImpl
    ): EncryptionRepository = impl
} 