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
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SafeChatDatabase = Room.databaseBuilder(
        context,
        SafeChatDatabase::class.java,
        "safechat.db"
    ).build()
    
    @Provides
    fun provideContactDao(database: SafeChatDatabase): ContactDao = database.contactDao()
    
    @Provides
    fun provideMessageDao(database: SafeChatDatabase): MessageDao = database.messageDao()
    
    @Provides
    @Singleton
    fun provideContactRepository(contactDao: ContactDao): ContactRepository =
        ContactRepositoryImpl(contactDao)
    
    @Provides
    @Singleton
    fun provideMessageRepository(messageDao: MessageDao): MessageRepository =
        MessageRepositoryImpl(messageDao)
        
    @Provides
    @Singleton
    fun provideEncryptionRepository(): EncryptionRepository =
        EncryptionRepositoryImpl()
}