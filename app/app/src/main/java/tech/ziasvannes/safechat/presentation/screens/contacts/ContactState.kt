package tech.ziasvannes.safechat.presentation.screens.contacts

import tech.ziasvannes.safechat.data.models.Contact

/**
 * State class for the Contact List screen
 */
data class ContactState(
    val contacts: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val filteredContacts: List<Contact> = emptyList()
)