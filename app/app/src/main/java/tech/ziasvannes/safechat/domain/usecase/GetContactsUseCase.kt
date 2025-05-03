package tech.ziasvannes.safechat.domain.usecase

import kotlinx.coroutines.flow.Flow
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import javax.inject.Inject

open class GetContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    /**
         * Returns a flow emitting updates to the list of contacts.
         *
         * Retrieves the current list of contacts as a reactive stream, emitting new lists whenever the underlying data changes.
         *
         * @return A [Flow] that emits lists of [Contact] objects.
         */
        open suspend operator fun invoke(): Flow<List<Contact>> =
        contactRepository.getContacts()
}