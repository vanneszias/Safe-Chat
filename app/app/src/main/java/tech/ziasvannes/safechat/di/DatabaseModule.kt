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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provides a singleton instance of the SafeChat Room database.
     *
     * @param context The application context used to initialize the database.
     * @return The singleton SafeChatDatabase instance.
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
     * Provides the ContactDao instance from the SafeChatDatabase.
     *
     * @return The ContactDao for accessing contact-related database operations.
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
         * Provides a singleton instance of ContactRepository using the given ContactDao.
         *
         * @return A ContactRepository implementation backed by the provided ContactDao.
         */
        @Provides
    @Singleton
    fun provideContactRepository(contactDao: ContactDao): ContactRepository =
        ContactRepositoryImpl(contactDao)
    
    /**
         * Provides a singleton instance of MessageRepository using the given MessageDao.
         *
         * @param messageDao The DAO for accessing message data in the database.
         * @return A MessageRepository implementation backed by the provided MessageDao.
         */
        @Provides
    @Singleton
    fun provideMessageRepository(messageDao: MessageDao): MessageRepository =
        MessageRepositoryImpl(messageDao)
        
    /**
         * Provides a singleton instance of the encryption repository for handling encryption-related operations.
         *
         * @return An instance of EncryptionRepository.
         */
        @Provides
    @Singleton
    fun provideEncryptionRepository(): EncryptionRepository =
        EncryptionRepositoryImpl()
}