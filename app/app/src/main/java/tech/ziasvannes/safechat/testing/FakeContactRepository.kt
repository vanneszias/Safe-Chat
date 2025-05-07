package tech.ziasvannes.safechat.testing

import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.data.models.ContactStatus
import tech.ziasvannes.safechat.domain.repository.ContactRepository

/**
 * A fake implementation of the ContactRepository that provides test data.
 *
 * This repository is useful for testing and UI development without requiring connection to an
 * actual backend.
 */
class FakeContactRepository @Inject constructor() : ContactRepository {

    // Generate initial contact list
    private val contacts =
            TestDataGenerator.generateContacts(15).toMutableList().apply {
                val selfId = TestDataGenerator.currentUserId
                if (none { it.id == selfId }) {
                    add(
                            Contact(
                                    id = selfId,
                                    name = TestDataGenerator.currentUserName,
                                    publicKey =
                                            "MIIBCgKCAQEA_SELF_KEY", // Placeholder, should be real
                                    // key if available
                                    lastSeen = System.currentTimeMillis(),
                                    status = ContactStatus.ONLINE,
                                    avatarUrl = null
                            )
                    )
                }
            }

    // Observable flow of contacts
    private val contactsFlow = MutableStateFlow(contacts.toList())

    /**
     * Returns a flow that emits the current list of all contacts and updates whenever the contact
     * list changes.
     *
     * @return A [Flow] emitting snapshots of the contact list.
     */
    override suspend fun getContacts(): Flow<List<Contact>> = contactsFlow

    /**
     * Adds the specified contact to the in-memory repository and updates observers.
     *
     * @param contact The contact to add.
     */
    override suspend fun addContact(contact: Contact) {
        contacts.add(contact)
        updateFlow()
    }

    /**
     * Replaces an existing contact with updated information if the contact exists.
     *
     * If a contact with the same ID as the provided contact is found, it is updated and observers
     * are notified of the change.
     */
    override suspend fun updateContact(contact: Contact) {
        val index = contacts.indexOfFirst { it.id == contact.id }
        if (index != -1) {
            contacts[index] = contact
            updateFlow()
        }
    }

    /**
     * Deletes the contact with the given UUID from the repository.
     *
     * If no contact with the specified ID exists, the repository remains unchanged.
     *
     * @param contactId The unique identifier of the contact to remove.
     */
    override suspend fun deleteContact(contactId: UUID) {
        contacts.removeIf { it.id == contactId }
        updateFlow()
    }

    /**
     * Returns the contact with the specified UUID, or null if no such contact exists.
     *
     * @param id The unique identifier of the contact to retrieve.
     * @return The matching contact, or null if not found.
     */
    override suspend fun getContactById(id: UUID): Contact? {
        return contacts.find { it.id == id }
    }

    /**
     * Returns a flow emitting lists of contacts whose names contain the given query string,
     * case-insensitive.
     *
     * @param query The search string to match against contact names.
     * @return A flow emitting filtered lists of contacts matching the query.
     */
    override suspend fun searchContacts(query: String): Flow<List<Contact>> {
        return contactsFlow.map { contactList ->
            contactList.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    /**
     * Emits the latest snapshot of the contacts list to observers.
     *
     * Updates the contacts flow with a fresh copy of the current contacts, ensuring subscribers
     * receive the most recent data.
     */
    private fun updateFlow() {
        contactsFlow.value = contacts.toList()
    }
}
