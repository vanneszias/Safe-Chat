package tech.ziasvannes.safechat.domain.repository

import kotlinx.coroutines.flow.Flow
import tech.ziasvannes.safechat.data.models.Contact
import java.util.UUID

interface ContactRepository {
    /**
 * Returns a flow emitting the current list of all contacts.
 *
 * The flow updates whenever the underlying contact data changes.
 *
 * @return A [Flow] that emits lists of [Contact] objects.
 */
suspend fun getContacts(): Flow<List<Contact>>
    /**
 * Adds a new contact to the repository.
 *
 * @param contact The contact to be added.
 */
suspend fun addContact(contact: Contact)
    /**
 * Updates the details of an existing contact.
 *
 * @param contact The contact entity with updated information.
 */
suspend fun updateContact(contact: Contact)
    /**
 * Removes the contact with the specified UUID from the repository.
 *
 * @param contactId The unique identifier of the contact to delete.
 */
suspend fun deleteContact(contactId: UUID)
    /**
 * Retrieves a contact by its unique identifier.
 *
 * @param id The UUID of the contact to retrieve.
 * @return The contact with the specified UUID, or null if not found.
 */
suspend fun getContactById(id: UUID): Contact?
    /**
 * Returns a flow emitting lists of contacts whose information matches the given search query.
 *
 * @param query The search string used to filter contacts.
 * @return A flow emitting updated lists of matching contacts as the underlying data changes.
 */
suspend fun searchContacts(query: String): Flow<List<Contact>>
}