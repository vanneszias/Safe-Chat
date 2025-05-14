package tech.ziasvannes.safechat.session

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSession @Inject constructor() {
    var userId: UUID? = null
    var userPublicKey: String? = null
} 