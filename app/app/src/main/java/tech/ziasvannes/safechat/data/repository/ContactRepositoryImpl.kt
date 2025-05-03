package tech.ziasvannes.safechat.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tech.ziasvannes.safechat.data.local.dao.ContactDao
import tech.ziasvannes.safechat.data.local.entity.ContactEntity
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import java.util.UUID
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao
) : ContactRepository {
    /**
         * Returns a flow emitting the current list of all contacts.
         *
         * The flow updates whenever the underlying contact data changes.
         *
         * @return A [Flow] emitting lists of [Contact] objects.
         */
        override suspend fun getContacts(): Flow<List<Contact>> =
        contactDao.getAllContacts().map { entities ->
            entities.map { it.toContact() }
        }

    /**
     * Adds a new contact to the data source.
     *
     * Converts the provided domain contact to an entity and inserts it using the DAO.
     *
     * @param contact The contact to be added.
     */
    override suspend fun addContact(contact: Contact) {
        contactDao.insertContact(ContactEntity.fromContact(contact))
    }

    /**
     * Updates an existing contact in the data source.
     *
     * Replaces the stored contact data with the values from the provided [contact].
     */
    override suspend fun updateContact(contact: Contact) {
        contactDao.updateContact(ContactEntity.fromContact(contact))
    }

    /**
     * Deletes a contact with the specified UUID if it exists.
     *
     * If a contact with the given ID is found, it is removed from the data source.
     *
     * @param contactId The unique identifier of the contact to delete.
     */
    override suspend fun deleteContact(contactId: UUID) {
        contactDao.getContactById(contactId.toString())?.let { 
            contactDao.deleteContact(it)
        }
    }

    /**
         * Retrieves a contact by its unique identifier.
         *
         * @param id The UUID of the contact to retrieve.
         * @return The contact if found, or null if no contact with the given ID exists.
         */
        override suspend fun getContactById(id: UUID): Contact? =
        contactDao.getContactById(id.toString())?.toContact()

    /**
         * Returns a flow emitting lists of contacts whose fields match the given search query.
         *
         * The search is delegated to the data access object and results are mapped to domain models.
         *
         * @param query The search string to filter contacts.
         * @return A flow emitting lists of matching contacts.
         */
        override suspend fun searchContacts(query: String): Flow<List<Contact>> =
        contactDao.searchContacts(query).map { entities ->
            entities.map { it.toContact() }
        }
}