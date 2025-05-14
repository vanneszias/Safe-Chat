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
     * Returns a reactive stream of all contacts in the database.
     *
     * @return A [Flow] emitting the current list of all [ContactEntity] objects whenever the data
     * changes.
     */
    @Query("SELECT * FROM contacts") fun getAllContacts(): Flow<List<ContactEntity>>

    /**
     * Retrieves a contact by its unique ID.
     *
     * @param contactId The unique identifier of the contact to retrieve.
     * @return The contact with the specified ID, or null if not found.
     */
    @Query("SELECT * FROM contacts WHERE id = :contactId")
    suspend fun getContactById(contactId: String): ContactEntity?

    /**
     * Returns a reactive stream of contacts whose names contain the specified query substring.
     *
     * @param query The substring to search for within contact names.
     * @return A [Flow] emitting lists of matching [ContactEntity] objects.
     */
    @Query("SELECT * FROM contacts WHERE name LIKE '%' || :query || '%'")
    fun searchContacts(query: String): Flow<List<ContactEntity>>

    /**
     * Inserts a contact into the database, replacing any existing entry with the same primary key.
     *
     * @param contact The contact entity to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    /**
     * Updates an existing contact in the database with new values.
     *
     * If the contact does not exist, no changes are made.
     */
    @Update suspend fun updateContact(contact: ContactEntity)

    /**
     * Deletes the specified contact from the database.
     *
     * @param contact The contact entity to remove.
     */
    @Delete suspend fun deleteContact(contact: ContactEntity)
}
