package tech.ziasvannes.safechat.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import tech.ziasvannes.safechat.testing.TestMode
import javax.inject.Named
import javax.inject.Singleton

/**
 * Provides the appropriate repository implementations based on the current TestMode configuration.
 * 
 * This module selects between real repositories (for production use) and test repositories
 * (for UI testing and development without a backend) based on TestMode.useTestRepositories.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Provides the appropriate ContactRepository implementation.
     */
    @Provides
    @Singleton
    fun provideContactRepository(
        @Named("real") realRepository: ContactRepository,
        @Named("test") testRepository: ContactRepository
    ): ContactRepository {
        return if (TestMode.useTestRepositories) testRepository else realRepository
    }

    /**
     * Provides the appropriate MessageRepository implementation.
     */
    @Provides
    @Singleton
    fun provideMessageRepository(
        @Named("real") realRepository: MessageRepository,
        @Named("test") testRepository: MessageRepository
    ): MessageRepository {
        return if (TestMode.useTestRepositories) testRepository else realRepository
    }

    /**
     * Provides the appropriate EncryptionRepository implementation.
     */
    @Provides
    @Singleton
    fun provideEncryptionRepository(
        @Named("real") realRepository: EncryptionRepository,
        @Named("test") testRepository: EncryptionRepository
    ): EncryptionRepository {
        return if (TestMode.useTestRepositories) testRepository else realRepository
    }
}