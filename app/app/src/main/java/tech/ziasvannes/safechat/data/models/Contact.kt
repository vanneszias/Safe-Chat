package tech.ziasvannes.safechat.data.models

import java.util.UUID

data class Contact(
        val id: UUID,
        val name: String,
        val publicKey: String,
        val lastSeen: Long,
        val status: ContactStatus,
        val avatar: String? = null
)

enum class ContactStatus {
    ONLINE,
    OFFLINE,
    AWAY
}
