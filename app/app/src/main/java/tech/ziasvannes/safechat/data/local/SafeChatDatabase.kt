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
    /**
 * Provides access to contact-related database operations.
 *
 * @return The DAO for managing contact entities.
 */
abstract fun contactDao(): ContactDao
    /**
 * Provides access to database operations for message entities.
 *
 * @return The DAO for performing CRUD operations on messages.
 */
abstract fun messageDao(): MessageDao
}