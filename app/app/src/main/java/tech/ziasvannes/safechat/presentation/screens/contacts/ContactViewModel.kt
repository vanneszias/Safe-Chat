package tech.ziasvannes.safechat.presentation.screens.contacts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.usecase.GetContactsUseCase
import tech.ziasvannes.safechat.domain.usecase.GetOrCreateChatSessionUseCase

@HiltViewModel
open class ContactViewModel
@Inject
constructor(
        private val getContactsUseCase: GetContactsUseCase,
        private val contactRepository: ContactRepository,
        private val getOrCreateChatSessionUseCase: GetOrCreateChatSessionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ContactState())
    open val state: StateFlow<ContactState> = _state.asStateFlow()

    init {
        onEvent(ContactEvent.LoadContacts)
    }

    /**
     * Handles contact-related events and updates the ViewModel state accordingly.
     *
     * Processes events such as loading contacts, updating the search query, deleting contacts, and
     * clearing errors. Navigation-related events are acknowledged but not handled within this
     * method.
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
     * Retrieves the list of contacts and updates the UI state with the results or any errors.
     *
     * Sets the loading state, collects contacts from the use case, and updates both the full and filtered contact lists based on the current search query. If an error occurs during retrieval, updates the state with an error message.
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
                                val filteredList =
                                        if (it.searchQuery.isBlank()) {
                                            contacts
                                        } else {
                                            contacts.filter { contact ->
                                                contact.name.contains(
                                                        it.searchQuery,
                                                        ignoreCase = true
                                                )
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
                    it.copy(isLoading = false, error = e.message ?: "An unknown error occurred")
                }
            }
        }
    }

    /**
     * Updates the search query and filters the contacts in the state accordingly.
     *
     * If the query is blank, all contacts are shown; otherwise, only contacts whose names contain the query (case-insensitive) are included in the filtered list.
     *
     * @param query The search string to filter contacts by name.
     */
    private fun updateSearchQuery(query: String) {
        _state.update { currentState ->
            val filteredList =
                    if (query.isBlank()) {
                        currentState.contacts
                    } else {
                        currentState.contacts.filter { contact ->
                            contact.name.contains(query, ignoreCase = true)
                        }
                    }

            currentState.copy(searchQuery = query, filteredContacts = filteredList)
        }
    }

    /**
     * Deletes a contact by its unique identifier and updates the state with an error message if the operation fails.
     *
     * @param contactId The unique identifier of the contact to delete.
     */
    private fun deleteContact(contactId: java.util.UUID) {
        viewModelScope.launch {
            try {
                contactRepository.deleteContact(contactId)
                // Contact will be removed from the list through the Flow
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to delete contact") }
            }
        }
    }

    /**
     * Initiates or retrieves a chat session for the specified contact and triggers navigation to the chat session.
     *
     * @param contact The contact for whom to start or retrieve a chat session.
     * @param onNavigate Callback invoked with the chat session ID to perform navigation.
     */
    fun startChatWithContact(contact: Contact, onNavigate: (UUID) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d(
                        "ContactViewModel",
                        "startChatWithContact called for contact: ${contact.name} (${contact.id})"
                )
                val chatSession = getOrCreateChatSessionUseCase(contact.id)
                Log.d("ContactViewModel", "Navigating to chat session: ${chatSession.id}")
                try {
                    onNavigate(chatSession.id)
                    Log.d(
                            "ContactViewModel",
                            "onNavigate called with chatSession.id: ${chatSession.id}"
                    )
                } catch (navEx: Exception) {
                    Log.e("ContactViewModel", "Exception in onNavigate", navEx)
                }
            } catch (e: Exception) {
                Log.e("ContactViewModel", "Failed to get or create chat session: ${e.message}", e)
            }
        }
    }
}
