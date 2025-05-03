package tech.ziasvannes.safechat.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tech.ziasvannes.safechat.data.local.dao.ContactDao
import tech.ziasvannes.safechat.data.local.entity.ContactEntity
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import java.util.UUID
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao
) : ContactRepository {
    override suspend fun getContacts(): Flow<List<Contact>> =
        contactDao.getAllContacts().map { entities ->
            entities.map { it.toContact() }
        }

    override suspend fun addContact(contact: Contact) {
        contactDao.insertContact(ContactEntity.fromContact(contact))
    }

    override suspend fun updateContact(contact: Contact) {
        contactDao.updateContact(ContactEntity.fromContact(contact))
    }

    override suspend fun deleteContact(contactId: UUID) {
        contactDao.getContactById(contactId.toString())?.let { 
            contactDao.deleteContact(it)
        }
    }

    override suspend fun getContactById(id: UUID): Contact? =
        contactDao.getContactById(id.toString())?.toContact()

    override suspend fun searchContacts(query: String): Flow<List<Contact>> =
        contactDao.searchContacts(query).map { entities ->
            entities.map { it.toContact() }
        }
}