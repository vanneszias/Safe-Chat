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
                 * Returns a flow that emits the current list of all contacts as domain models.
                 *
                 * Each emission reflects updates to the underlying contact data source.
                 *
                 * @return A [Flow] emitting lists of [Contact] objects.
                 */
        override suspend fun getContacts(): Flow<List<Contact>> =
                contactDao.getAllContacts().map { entities -> entities.map { it.toContact() } }

        /**
         * Inserts a new contact into the data source.
         *
         * Converts the provided domain contact model to a data entity and delegates insertion to the DAO.
         */
        override suspend fun addContact(contact: Contact) {
                contactDao.insertContact(ContactEntity.fromContact(contact))
        }

        /**
         * Updates an existing contact in the data source with new information.
         *
         * @param contact The contact containing updated details.
         */
        override suspend fun updateContact(contact: Contact) {
                contactDao.updateContact(ContactEntity.fromContact(contact))
        }

        /**
         * Removes the contact with the specified UUID from the data source if it exists.
         *
         * If no contact with the given ID is found, no action is taken.
         *
         * @param contactId The unique identifier of the contact to remove.
         */
        override suspend fun deleteContact(contactId: UUID) {
                contactDao.getContactById(contactId.toString())?.let {
                        contactDao.deleteContact(it)
                }
        }

        /**
                 * Returns the contact with the specified UUID, or null if not found.
                 *
                 * @param id The unique identifier of the contact.
                 * @return The corresponding contact, or null if no contact exists with the given ID.
                 */
        override suspend fun getContactById(id: UUID): Contact? =
                contactDao.getContactById(id.toString())?.toContact()

        /**
                 * Returns a flow of contact lists matching the specified search query.
                 *
                 * The flow emits updated lists whenever the underlying data changes.
                 *
                 * @param query The search string to filter contacts by name or other relevant fields.
                 * @return A flow emitting lists of contacts that match the query.
                 */
        override suspend fun searchContacts(query: String): Flow<List<Contact>> =
                contactDao.searchContacts(query).map { entities -> entities.map { it.toContact() } }
}

/**
         * Converts this [ContactEntity] to a [Contact] domain model.
         *
         * Maps all relevant fields, converting the string ID to a [UUID].
         *
         * @return The corresponding [Contact] instance.
         */
        private fun ContactEntity.toContact(): Contact =
        Contact(
                id = UUID.fromString(id),
                name = name,
                publicKey = publicKey,
                lastSeen = lastSeen,
                status = status,
                avatar = avatar
        )
