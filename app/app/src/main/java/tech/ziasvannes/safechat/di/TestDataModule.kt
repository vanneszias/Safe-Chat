package tech.ziasvannes.safechat.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import tech.ziasvannes.safechat.testing.FakeContactRepository
import tech.ziasvannes.safechat.testing.FakeMessageRepository
import tech.ziasvannes.safechat.presentation.preview.FakeEncryptionRepository
import javax.inject.Named
import javax.inject.Singleton

/**
 * Provides fake repository implementations for testing and UI development.
 * 
 * These repositories generate realistic test data that can be used to visualize
 * and test the UI without requiring a working backend API.
 */
@Module
@InstallIn(SingletonComponent::class)
object TestDataModule {
    
    /**
     * Provides a fake contact repository filled with test data.
     */
    @Provides
    @Singleton
    @Named("test")
    fun provideFakeContactRepository(): ContactRepository = FakeContactRepository()
    
    /**
     * Provides a fake message repository with generated test messages and chat sessions.
     */
    @Provides
    @Singleton
    @Named("test")
    fun provideFakeMessageRepository(
        @Named("test") contactRepository: ContactRepository
    ): MessageRepository = FakeMessageRepository(contactRepository)
    
    /**
     * Provides a fake encryption repository for testing.
     */
    @Provides
    @Singleton
    @Named("test")
    fun provideFakeEncryptionRepository(): EncryptionRepository = FakeEncryptionRepository()
}