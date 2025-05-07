package tech.ziasvannes.safechat.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import tech.ziasvannes.safechat.presentation.preview.FakeEncryptionRepository
import tech.ziasvannes.safechat.testing.FakeContactRepository
import tech.ziasvannes.safechat.testing.FakeMessageRepository

/**
 * Provides fake repository implementations for testing and UI development.
 *
 * These repositories generate realistic test data that can be used to visualize and test the UI
 * without requiring a working backend API.
 */
@Module
@InstallIn(SingletonComponent::class)
object TestDataModule {

    /**
     * Supplies a singleton instance of a fake contact repository populated with test data for
     * testing and UI development.
     *
     * @return A `ContactRepository` implementation containing sample contacts.
     */
    @Provides
    @Singleton
    @Named("test")
    fun provideFakeContactRepository(): ContactRepository = FakeContactRepository()

    /**
     * Supplies a singleton instance of a fake message repository for testing, using the provided
     * fake contact repository to generate test messages and chat sessions.
     *
     * @param contactRepository The fake contact repository used to generate related test data.
     * @return A fake message repository containing test messages and chat sessions.
     */
    @Provides
    @Singleton
    fun provideFakeMessageRepository(
            @Named("test") contactRepository: ContactRepository
    ): FakeMessageRepository = FakeMessageRepository(contactRepository)

    /**
     * Supplies a singleton instance of a fake message repository for testing, using the provided
     * fake contact repository to generate test messages and chat sessions.
     *
     * @param fakeMessageRepository The fake message repository used to generate related test data.
     * @return A fake message repository containing test messages and chat sessions.
     */
    @Provides
    @Singleton
    @Named("test")
    fun provideFakeMessageRepositoryAsInterface(
            fakeMessageRepository: FakeMessageRepository
    ): MessageRepository = fakeMessageRepository

    /**
     * Supplies a singleton instance of a fake encryption repository for testing purposes.
     *
     * @return An instance of FakeEncryptionRepository used as EncryptionRepository for test
     * scenarios.
     */
    @Provides
    @Singleton
    @Named("test")
    fun provideFakeEncryptionRepository(): EncryptionRepository = FakeEncryptionRepository()
}
