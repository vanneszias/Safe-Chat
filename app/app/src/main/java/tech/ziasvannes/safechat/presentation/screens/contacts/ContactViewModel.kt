package tech.ziasvannes.safechat.presentation.screens.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.usecase.GetContactsUseCase
import javax.inject.Inject

@HiltViewModel
open class ContactViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ContactState())
    open val state: StateFlow<ContactState> = _state.asStateFlow()

    init {
        onEvent(ContactEvent.LoadContacts)
    }

    /**
     * Handles contact-related events and updates the ViewModel state accordingly.
     *
     * Processes events such as loading contacts, updating the search query, deleting contacts, and clearing errors.
     * Navigation-related events are acknowledged but not handled within this method.
     *
     * @param event The contact event to process.
     */
    open fun onEvent(event: ContactEvent) {
        when (event) {
            is ContactEvent.LoadContacts -> {
                loadContacts()
            }
            is ContactEvent.OnSearchQueryChanged -> {
                updateSearchQuery(event.query)
            }
            is ContactEvent.OnContactSelected -> {
                // Will be handled by navigation
            }
            is ContactEvent.OnAddContactClick -> {
                // Will be handled by navigation
            }
            is ContactEvent.OnDeleteContact -> {
                deleteContact(event.contactId)
            }
            is ContactEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    /**
     * Loads the list of contacts and updates the state with the results.
     *
     * Initiates contact retrieval, sets the loading state, and updates the state with the full and filtered contact lists.
     * If an error occurs during loading, updates the state with an error message.
     */
    private fun loadContacts() {
        _state.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                getContactsUseCase()
                    .catch { e ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "An unknown error occurred"
                            )
                        }
                    }
                    .onEach { contacts ->
                        _state.update {
                            val filteredList = if (it.searchQuery.isBlank()) {
                                contacts
                            } else {
                                contacts.filter { contact ->
                                    contact.name.contains(it.searchQuery, ignoreCase = true)
                                }
                            }
                            
                            it.copy(
                                contacts = contacts,
                                filteredContacts = filteredList,
                                isLoading = false
                            )
                        }
                    }
                    .launchIn(viewModelScope)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An unknown error occurred"
                    )
                }
            }
        }
    }

    /**
     * Updates the search query in the state and filters the contacts list based on the query.
     *
     * If the query is blank, all contacts are included in the filtered list; otherwise, only contacts whose names contain the query (case-insensitive) are included.
     *
     * @param query The new search query string to apply.
     */
    private fun updateSearchQuery(query: String) {
        _state.update { currentState ->
            val filteredList = if (query.isBlank()) {
                currentState.contacts
            } else {
                currentState.contacts.filter { contact ->
                    contact.name.contains(query, ignoreCase = true)
                }
            }
            
            currentState.copy(
                searchQuery = query,
                filteredContacts = filteredList
            )
        }
    }

    /**
     * Attempts to delete a contact by its unique identifier and updates the state with an error message if deletion fails.
     *
     * @param contactId The UUID of the contact to be deleted.
     */
    private fun deleteContact(contactId: java.util.UUID) {
        viewModelScope.launch {
            try {
                contactRepository.deleteContact(contactId)
                // Contact will be removed from the list through the Flow
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message ?: "Failed to delete contact")
                }
            }
        }
    }
}