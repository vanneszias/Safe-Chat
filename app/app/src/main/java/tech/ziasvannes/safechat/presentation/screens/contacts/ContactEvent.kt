package tech.ziasvannes.safechat.presentation.screens.contacts

import tech.ziasvannes.safechat.data.models.Contact
import java.util.UUID

/**
 * Events for the Contact List screen
 */
sealed class ContactEvent {
    /**
     * Load all contacts
     */
    object LoadContacts : ContactEvent()
    
    /**
     * Update search query and filter contacts
     */
    data class OnSearchQueryChanged(val query: String) : ContactEvent()
    
    /**
     * Select a contact to start a chat with
     */
    data class OnContactSelected(val contact: Contact) : ContactEvent()
    
    /**
     * Add a new contact
     */
    object OnAddContactClick : ContactEvent()
    
    /**
     * Delete a contact
     */
    data class OnDeleteContact(val contactId: UUID) : ContactEvent()
    
    /**
     * Clear any error messages
     */
    object ClearError : ContactEvent()
}