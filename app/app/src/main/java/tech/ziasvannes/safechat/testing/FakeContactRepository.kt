package tech.ziasvannes.safechat.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import java.util.UUID
import javax.inject.Inject

/**
 * A fake implementation of the ContactRepository that provides test data.
 * 
 * This repository is useful for testing and UI development without requiring
 * connection to an actual backend.
 */
class FakeContactRepository @Inject constructor() : ContactRepository {
    
    // Generate initial contact list
    private val contacts = TestDataGenerator.generateContacts(15).toMutableList()
    
    // Observable flow of contacts
    private val contactsFlow = MutableStateFlow(contacts.toList())
    
    /**
     * Returns a flow emitting the current list of all contacts.
     */
    override suspend fun getContacts(): Flow<List<Contact>> = contactsFlow
    
    /**
     * Adds a new contact to the repository.
     */
    override suspend fun addContact(contact: Contact) {
        contacts.add(contact)
        updateFlow()
    }
    
    /**
     * Updates the details of an existing contact.
     */
    override suspend fun updateContact(contact: Contact) {
        val index = contacts.indexOfFirst { it.id == contact.id }
        if (index != -1) {
            contacts[index] = contact
            updateFlow()
        }
    }
    
    /**
     * Removes the contact with the specified UUID from the repository.
     */
    override suspend fun deleteContact(contactId: UUID) {
        contacts.removeIf { it.id == contactId }
        updateFlow()
    }
    
    /**
     * Retrieves a contact by its unique identifier.
     */
    override suspend fun getContactById(id: UUID): Contact? {
        return contacts.find { it.id == id }
    }
    
    /**
     * Returns a flow emitting lists of contacts matching the search query.
     */
    override suspend fun searchContacts(query: String): Flow<List<Contact>> {
        return contactsFlow.map { contactList ->
            contactList.filter { 
                it.name.contains(query, ignoreCase = true)
            }
        }
    }
    
    /**
     * Updates the flow with the current list of contacts.
     */
    private fun updateFlow() {
        contactsFlow.value = contacts.toList()
    }
}