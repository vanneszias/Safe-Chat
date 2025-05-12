package tech.ziasvannes.safechat.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import tech.ziasvannes.safechat.data.local.entity.ContactEntity

@Dao
interface ContactDao {
    /**
 * Returns a reactive stream emitting the list of all contacts in the database.
 *
 * The returned [Flow] emits an updated list of [ContactEntity] objects whenever the contacts table changes.
 *
 * @return A [Flow] of the current list of all contacts.
 */
    @Query("SELECT * FROM contacts") fun getAllContacts(): Flow<List<ContactEntity>>

    /**
     * Returns the contact with the specified unique ID, or null if no such contact exists.
     *
     * @param contactId The unique identifier of the contact to retrieve.
     * @return The matching contact, or null if not found.
     */
    @Query("SELECT * FROM contacts WHERE id = :contactId")
    suspend fun getContactById(contactId: String): ContactEntity?

    /**
     * Returns a reactive stream of contacts whose names contain the given substring.
     *
     * The stream emits updated lists whenever the underlying contact data changes.
     *
     * @param query Substring to match within contact names.
     * @return A [Flow] emitting lists of [ContactEntity] objects whose names contain the query.
     */
    @Query("SELECT * FROM contacts WHERE name LIKE '%' || :query || '%'")
    fun searchContacts(query: String): Flow<List<ContactEntity>>

    /**
     * Inserts a contact into the database, replacing any existing contact with the same primary key.
     *
     * If a contact with the same primary key already exists, it will be overwritten.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    /**
 * Updates the specified contact in the database with new values.
 *
 * If the contact does not exist, no update is performed.
 */
    @Update suspend fun updateContact(contact: ContactEntity)

    /**
 * Removes the specified contact from the database.
 *
 * @param contact The contact entity to delete.
 */
    @Delete suspend fun deleteContact(contact: ContactEntity)

    /**
 * Returns the total number of contacts in the database.
 *
 * @return The count of contact entries.
 */
@Query("SELECT COUNT(*) FROM contacts") suspend fun getCount(): Int
}
