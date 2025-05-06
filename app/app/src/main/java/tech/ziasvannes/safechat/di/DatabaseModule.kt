package tech.ziasvannes.safechat.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import tech.ziasvannes.safechat.data.local.SafeChatDatabase
import tech.ziasvannes.safechat.data.local.dao.ContactDao
import tech.ziasvannes.safechat.data.local.dao.MessageDao
import tech.ziasvannes.safechat.data.repository.ContactRepositoryImpl
import tech.ziasvannes.safechat.data.repository.EncryptionRepositoryImpl
import tech.ziasvannes.safechat.data.repository.MessageRepositoryImpl
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provides a singleton instance of the SafeChat Room database.
     *
     * @return The application's SafeChatDatabase configured with the name "safechat.db".
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SafeChatDatabase = Room.databaseBuilder(
        context,
        SafeChatDatabase::class.java,
        "safechat.db"
    ).build()
    
    /**
     * Provides an instance of ContactDao from the SafeChatDatabase.
     *
     * @param database The Room database instance used to access DAOs.
     * @return The ContactDao for performing contact-related database operations.
     */
    @Provides
    fun provideContactDao(database: SafeChatDatabase): ContactDao = database.contactDao()
    
    /**
     * Provides the MessageDao instance from the SafeChatDatabase.
     *
     * @param database The Room database instance for SafeChat.
     * @return The MessageDao for accessing message-related database operations.
     */
    @Provides
    fun provideMessageDao(database: SafeChatDatabase): MessageDao = database.messageDao()
    
    /**
     * Provides a singleton instance of the real ContactRepository implementation.
     *
     * @param contactDao The DAO used for contact data operations.
     * @return A real ContactRepository for managing contact-related data.
     */
    @Provides
    @Singleton
    @Named("real")
    fun provideContactRepository(contactDao: ContactDao): ContactRepository =
        ContactRepositoryImpl(contactDao)
    
    /**
     * Provides a singleton instance of the real MessageRepository implementation.
     *
     * @param messageDao The DAO used for message data access.
     * @return A real MessageRepository implementation for managing message data.
     */
    @Provides
    @Singleton
    @Named("real")
    fun provideMessageRepository(messageDao: MessageDao): MessageRepository =
        MessageRepositoryImpl(messageDao)
        
    /**
     * Provides a singleton instance of the real encryption repository implementation.
     *
     * @return A real implementation of [EncryptionRepository].
     */
    @Provides
    @Singleton
    @Named("real")
    fun provideEncryptionRepository(): EncryptionRepository =
        EncryptionRepositoryImpl()
}