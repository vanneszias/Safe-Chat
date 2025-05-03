package tech.ziasvannes.safechat.domain.usecase

import kotlinx.coroutines.flow.Flow
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import javax.inject.Inject

open class GetContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    /**
         * Returns a reactive stream of contact lists, emitting updates whenever the contact data changes.
         *
         * @return A [Flow] emitting the current list of [Contact] objects and subsequent updates.
         */
        open suspend operator fun invoke(): Flow<List<Contact>> =
        contactRepository.getContacts()
}