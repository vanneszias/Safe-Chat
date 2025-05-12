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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    /**
         * Provides a singleton instance of the SafeChatDatabase using the application context.
         *
         * Builds and returns a Room database named "safe_chat_database".
         *
         * @return The singleton SafeChatDatabase instance.
         */
        @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): SafeChatDatabase =
        Room.databaseBuilder(
            appContext,
            SafeChatDatabase::class.java,
            "safe_chat_database"
        ).build()

    /**
     * Provides the singleton instance of ContactDao from the SafeChatDatabase.
     *
     * @return The ContactDao for accessing contact-related database operations.
     */
    @Provides
    @Singleton
    fun provideContactDao(db: SafeChatDatabase): ContactDao = db.contactDao()

    /**
     * Provides the singleton instance of MessageDao from the SafeChatDatabase.
     *
     * @return The MessageDao for accessing message-related database operations.
     */
    @Provides
    @Singleton
    fun provideMessageDao(db: SafeChatDatabase): MessageDao = db.messageDao()
} 