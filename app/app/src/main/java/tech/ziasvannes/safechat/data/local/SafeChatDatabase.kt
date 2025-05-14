package tech.ziasvannes.safechat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import tech.ziasvannes.safechat.data.local.dao.ContactDao
import tech.ziasvannes.safechat.data.local.dao.MessageDao
import tech.ziasvannes.safechat.data.local.entity.ContactEntity
import tech.ziasvannes.safechat.data.local.entity.MessageEntity

@Database(
        entities = [ContactEntity::class, MessageEntity::class],
        version = 2,
        exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SafeChatDatabase : RoomDatabase() {
    /**
     * Provides access to contact-related database operations.
     *
     * @return The DAO for performing CRUD operations on contacts.
     */
    abstract fun contactDao(): ContactDao
    /**
     * Provides access to database operations related to messages.
     *
     * @return The DAO for performing CRUD operations on message entities.
     */
    abstract fun messageDao(): MessageDao
}
