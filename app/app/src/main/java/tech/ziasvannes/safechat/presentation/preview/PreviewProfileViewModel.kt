package tech.ziasvannes.safechat.presentation.preview

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import tech.ziasvannes.safechat.data.remote.ApiService
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import tech.ziasvannes.safechat.presentation.screens.profile.ProfileViewModel
import tech.ziasvannes.safechat.session.UserSession

/**
 * A preview-specific version of the ProfileViewModel that uses a fake repository. This is only for
 * UI previews and is not used in the actual app.
 */

// EntryPoint to access ApiService from Hilt in previews
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface ApiServiceEntryPoint {
    fun apiService(): ApiService
    fun encryptionRepository(): EncryptionRepository
    fun userSession(): UserSession
}

fun getApiService(context: Context): ApiService {
    val entryPoint = EntryPointAccessors.fromApplication(context, ApiServiceEntryPoint::class.java)
    return entryPoint.apiService()
}

fun getEncryptionRepository(context: Context): EncryptionRepository {
    val entryPoint = EntryPointAccessors.fromApplication(context, ApiServiceEntryPoint::class.java)
    return entryPoint.encryptionRepository()
}

fun getUserSession(context: Context): UserSession {
    val entryPoint = EntryPointAccessors.fromApplication(context, ApiServiceEntryPoint::class.java)
    return entryPoint.userSession()
}

class PreviewProfileViewModel(context: Context) :
        ProfileViewModel(
                getApiService(context),
                getEncryptionRepository(context),
                getUserSession(context)
        )
