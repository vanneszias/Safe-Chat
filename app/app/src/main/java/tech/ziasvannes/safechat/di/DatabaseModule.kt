package tech.ziasvannes.safechat.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import tech.ziasvannes.safechat.data.local.SafeChatDatabase
import tech.ziasvannes.safechat.data.local.dao.ContactDao
import tech.ziasvannes.safechat.data.local.dao.MessageDao
import tech.ziasvannes.safechat.data.repository.ContactRepositoryImpl
import tech.ziasvannes.safechat.data.repository.EncryptionRepositoryImpl
import tech.ziasvannes.safechat.data.repository.MessageRepositoryImpl
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import tech.ziasvannes.safechat.domain.repository.MessageRepository

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
        fun provideDatabase(@ApplicationContext context: Context): SafeChatDatabase =
                Room.databaseBuilder(context, SafeChatDatabase::class.java, "safechat.db").build()

        /**
         * Provides an instance of ContactDao from the SafeChatDatabase.
         *
         * @param database The Room database instance used to access DAOs.
         * @return The ContactDao for performing contact-related database operations.
         */
        @Provides
        fun provideContactDao(database: SafeChatDatabase): ContactDao = database.contactDao()

        /**
         * Provides the MessageDao for accessing message data in the SafeChat database.
         *
         * @param database The SafeChatDatabase instance from which to obtain the DAO.
         * @return The MessageDao used for message-related database operations.
         */
        @Provides
        fun provideMessageDao(database: SafeChatDatabase): MessageDao = database.messageDao()

        /**
         * Provides the singleton "real" implementation of ContactRepository for dependency
         * injection.
         *
         * @return The ContactRepository implementation that manages contact data using the provided
         * ContactDao.
         */
        @Provides
        @Singleton
        @Named("real")
        fun provideContactRepository(contactDao: ContactDao): ContactRepository =
                ContactRepositoryImpl(contactDao)

        /**
         * Provides the singleton "real" implementation of MessageRepository for message data
         * management.
         *
         * @return The MessageRepository implementation backed by the provided MessageDao.
         */
        @Provides
        @Singleton
        @Named("real")
        fun provideMessageRepository(messageDao: MessageDao): MessageRepository =
                MessageRepositoryImpl(messageDao)

        /**
         * Provides the singleton "real" implementation of [EncryptionRepository].
         *
         * @return The real [EncryptionRepository] instance.
         */
        @Provides
        @Singleton
        @Named("real")
        fun provideEncryptionRepository(): EncryptionRepository = EncryptionRepositoryImpl()
}
