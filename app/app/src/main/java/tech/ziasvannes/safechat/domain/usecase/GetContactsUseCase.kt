package tech.ziasvannes.safechat.domain.usecase

import kotlinx.coroutines.flow.Flow
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import javax.inject.Inject

class GetContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(): Flow<List<Contact>> =
        contactRepository.getContacts()
}