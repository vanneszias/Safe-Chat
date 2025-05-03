package tech.ziasvannes.safechat.domain.usecase

import kotlinx.coroutines.flow.Flow
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import javax.inject.Inject

class GetContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    /**
         * Returns a flow emitting the current list of contacts.
         *
         * The flow emits updates whenever the contact list changes.
         *
         * @return A [Flow] that emits lists of [Contact] objects.
         */
        suspend operator fun invoke(): Flow<List<Contact>> =
        contactRepository.getContacts()
}