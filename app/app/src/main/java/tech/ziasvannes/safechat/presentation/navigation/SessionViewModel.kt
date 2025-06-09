package tech.ziasvannes.safechat.presentation.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.ziasvannes.safechat.session.UserSession
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    val userSession: UserSession
) : ViewModel()