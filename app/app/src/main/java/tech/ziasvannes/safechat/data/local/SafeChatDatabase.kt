package tech.ziasvannes.safechat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import tech.ziasvannes.safechat.data.local.dao.ContactDao
import tech.ziasvannes.safechat.data.local.dao.MessageDao
import tech.ziasvannes.safechat.data.local.entity.ContactEntity
import tech.ziasvannes.safechat.data.local.entity.MessageEntity

@Database(
    entities = [
        ContactEntity::class,
        MessageEntity::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class SafeChatDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao
}