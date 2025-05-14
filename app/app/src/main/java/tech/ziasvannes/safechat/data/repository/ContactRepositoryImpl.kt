package tech.ziasvannes.safechat.data.repository

import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tech.ziasvannes.safechat.data.local.dao.ContactDao
import tech.ziasvannes.safechat.data.local.entity.ContactEntity
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.domain.repository.ContactRepository

class ContactRepositoryImpl @Inject constructor(private val contactDao: ContactDao) :
        ContactRepository {
    /**
     * Returns a flow emitting the list of all contacts.
     *
     * Each emission reflects the current state of the contact database, with entities mapped to
     * domain models.
     *
     * @return A [Flow] that emits updated lists of [Contact] objects.
     */
    override suspend fun getContacts(): Flow<List<Contact>> =
            contactDao.getAllContacts().map { entities -> entities.map { it.toContact() } }

    /**
     * Adds a new contact to the data source.
     *
     * @param contact The contact to be added.
     */
    override suspend fun addContact(contact: Contact) {
        contactDao.insertContact(ContactEntity.fromContact(contact))
    }

    /**
     * Updates an existing contact in the data source with the provided contact information.
     *
     * @param contact The contact data to update.
     */
    override suspend fun updateContact(contact: Contact) {
        contactDao.updateContact(ContactEntity.fromContact(contact))
    }

    /**
     * Deletes a contact with the specified UUID if it exists.
     *
     * If a contact with the given ID is found, it is removed from the data source.
     *
     * @param contactId The UUID of the contact to delete.
     */
    override suspend fun deleteContact(contactId: UUID) {
        contactDao.getContactById(contactId.toString())?.let { contactDao.deleteContact(it) }
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
     * Returns a flow emitting lists of contacts that match the given search query.
     *
     * @param query The search string used to filter contacts.
     * @return A flow emitting lists of matching contacts.
     */
    override suspend fun searchContacts(query: String): Flow<List<Contact>> =
            contactDao.searchContacts(query).map { entities -> entities.map { it.toContact() } }
}
