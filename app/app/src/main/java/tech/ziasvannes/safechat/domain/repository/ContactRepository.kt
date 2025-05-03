package tech.ziasvannes.safechat.domain.repository

import kotlinx.coroutines.flow.Flow
import tech.ziasvannes.safechat.data.models.Contact
import java.util.UUID

interface ContactRepository {
    suspend fun getContacts(): Flow<List<Contact>>
    suspend fun addContact(contact: Contact)
    suspend fun updateContact(contact: Contact)
    suspend fun deleteContact(contactId: UUID)
    suspend fun getContactById(id: UUID): Contact?
    suspend fun searchContacts(query: String): Flow<List<Contact>>
}