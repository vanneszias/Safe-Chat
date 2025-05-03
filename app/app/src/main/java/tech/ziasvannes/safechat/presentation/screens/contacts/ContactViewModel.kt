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