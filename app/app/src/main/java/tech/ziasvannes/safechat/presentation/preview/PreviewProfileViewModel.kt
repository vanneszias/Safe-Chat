package tech.ziasvannes.safechat.presentation.preview

import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.presentation.screens.profile.ProfileViewModel

/**
 * A preview-specific version of the ProfileViewModel that uses a fake repository.
 * This is only for UI previews and is not used in the actual app.
 */
class PreviewProfileViewModel(contactRepository: ContactRepository) : ProfileViewModel(FakeEncryptionRepository(),
    contactRepository
)