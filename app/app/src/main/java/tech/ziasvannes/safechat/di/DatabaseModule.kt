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
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): SafeChatDatabase =
        Room.databaseBuilder(
            appContext,
            SafeChatDatabase::class.java,
            "safe_chat_database"
        ).build()

    @Provides
    @Singleton
    fun provideContactDao(db: SafeChatDatabase): ContactDao = db.contactDao()

    @Provides
    @Singleton
    fun provideMessageDao(db: SafeChatDatabase): MessageDao = db.messageDao()
} 