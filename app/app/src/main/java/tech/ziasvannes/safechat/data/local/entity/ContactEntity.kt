package tech.ziasvannes.safechat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.data.models.ContactStatus
import java.util.UUID

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val publicKey: String,
    val lastSeen: Long,
    val status: ContactStatus,
    val avatarUrl: String?
) {
    fun toContact(): Contact = Contact(
        id = UUID.fromString(id),
        name = name,
        publicKey = publicKey,
        lastSeen = lastSeen,
        status = status,
        avatarUrl = avatarUrl
    )

    companion object {
        fun fromContact(contact: Contact): ContactEntity = ContactEntity(
            id = contact.id.toString(),
            name = contact.name,
            publicKey = contact.publicKey,
            lastSeen = contact.lastSeen,
            status = contact.status,
            avatarUrl = contact.avatarUrl
        )
    }
}