package tech.ziasvannes.safechat.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import tech.ziasvannes.safechat.data.local.entity.MessageEntity
import tech.ziasvannes.safechat.data.models.MessageStatus

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE senderId = :chatId OR receiverId = :chatId ORDER BY timestamp DESC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
    
    @Update
    suspend fun updateMessage(message: MessageEntity)
    
    @Delete
    suspend fun deleteMessage(message: MessageEntity)
    
    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)
    
    @Query("""
        SELECT DISTINCT m.*
        FROM messages m
        WHERE m.id IN (
            SELECT m2.id
            FROM messages m2
            GROUP BY CASE 
                WHEN m2.senderId < m2.receiverId THEN m2.senderId || m2.receiverId
                ELSE m2.receiverId || m2.senderId
            END
            HAVING m2.timestamp = MAX(timestamp)
        )
        ORDER BY m.timestamp DESC
    """)
    fun getChatSessions(): Flow<List<MessageEntity>>
}