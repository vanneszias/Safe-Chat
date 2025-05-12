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
     * Returns a reactive stream of messages involving the specified chat participant.
     *
     * Emits lists of messages where the given chat ID matches either the sender or receiver, ordered by most recent first.
     *
     * @param chatId The participant's ID to filter messages.
     * @return A Flow emitting updated lists of relevant messages.
     */
    @Query(
            "SELECT * FROM messages WHERE senderId = :chatId OR receiverId = :chatId ORDER BY timestamp DESC"
    )
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    /**
     * Inserts a message into the database, replacing any existing message with the same primary key.
     *
     * If a message with the same primary key exists, it is overwritten by the new entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    /**
 * Updates a message in the database by replacing the existing entry with the provided entity.
 *
 * The message to update is identified by its primary key.
 */
    @Update suspend fun updateMessage(message: MessageEntity)

    /**
 * Deletes a message entity from the database.
 *
 * @param message The message entity to remove.
 */
    @Delete suspend fun deleteMessage(message: MessageEntity)

    /**
     * Sets the status of a specific message by its unique ID.
     *
     * @param messageId The unique identifier of the message.
     * @param status The status value to assign to the message.
     */
    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    /**
     * Returns a reactive stream of the most recent message from each unique chat session.
     *
     * A chat session is defined by the unordered pair of sender and receiver IDs. The stream emits an updated list whenever the latest message in any session changes, with results ordered by descending timestamp.
     *
     * @return A [Flow] emitting the latest [MessageEntity] from each chat session.
     */
    @Query(
            """
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
    """
    )
    fun getChatSessions(): Flow<List<MessageEntity>>

    /**
 * Returns the total number of messages stored in the database.
 *
 * @return The count of all message entities.
 */
@Query("SELECT COUNT(*) FROM messages") suspend fun getCount(): Int

    /**
     * Returns a reactive stream of messages exchanged between two users, ordered by most recent first.
     *
     * The stream emits updates whenever messages between the specified users change in the database.
     *
     * @param userId The unique identifier of one user in the conversation.
     * @param contactId The unique identifier of the other user in the conversation.
     * @return A Flow emitting lists of messages exchanged between the two users, ordered by descending timestamp.
     */
    @Query(
            """
        SELECT * FROM messages
        WHERE (senderId = :userId AND receiverId = :contactId)
           OR (senderId = :contactId AND receiverId = :userId)
        ORDER BY timestamp DESC
    """
    )
    fun getMessagesBetween(userId: String, contactId: String): Flow<List<MessageEntity>>
}
