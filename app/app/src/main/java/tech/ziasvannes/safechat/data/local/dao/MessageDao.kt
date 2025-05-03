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
    /**
     * Returns a reactive stream of messages where the given chat ID is either the sender or receiver, ordered by most recent first.
     *
     * @param chatId The ID of the chat participant to filter messages by.
     * @return A Flow emitting lists of messages involving the specified chat ID, sorted by descending timestamp.
     */
    @Query("SELECT * FROM messages WHERE senderId = :chatId OR receiverId = :chatId ORDER BY timestamp DESC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>
    
    /**
     * Inserts a message into the database, replacing any existing entry with the same primary key.
     *
     * If a message with the same primary key already exists, it will be overwritten.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
    
    /**
     * Updates an existing message entity in the database.
     *
     * If the message does not exist, no changes are made.
     */
    @Update
    suspend fun updateMessage(message: MessageEntity)
    
    /**
     * Deletes the specified message entity from the database.
     *
     * @param message The message entity to be removed.
     */
    @Delete
    suspend fun deleteMessage(message: MessageEntity)
    
    /**
     * Updates the status of a message identified by its ID.
     *
     * @param messageId The unique identifier of the message to update.
     * @param status The new status to set for the message.
     */
    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)
    
    /**
     * Returns a reactive stream of the latest message from each unique chat session.
     *
     * A chat session is defined as a unique pair of sender and receiver, regardless of message direction.
     * The returned list contains the most recent message for each session, ordered by timestamp descending.
     *
     * @return A Flow emitting a list of the latest messages per chat session.
     */
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