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
import tech.ziasvannes.safechat.data.repository.EncryptionRepositoryImpl
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository

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
         * Provides an instance of MessageDao for accessing message data in the SafeChat database.
         *
         * @return The MessageDao for message-related database operations.
         */
        @Provides
        fun provideMessageDao(database: SafeChatDatabase): MessageDao = database.messageDao()

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
