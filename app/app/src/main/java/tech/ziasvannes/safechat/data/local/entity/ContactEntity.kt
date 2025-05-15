package tech.ziasvannes.safechat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.data.models.ContactStatus

@Entity(tableName = "contacts")
data class ContactEntity(
        @PrimaryKey val id: String,
        val name: String,
        val publicKey: String,
        val lastSeen: Long,
        val status: ContactStatus,
        val avatar: String? = null
) {
        /**
                 * Converts a ContactEntity instance to a Contact domain model.
                 *
                 * Transforms the string-based ID to a UUID and maps all other fields directly.
                 *
                 * @return The corresponding Contact object.
                 */
        fun toContact(): Contact =
                Contact(
                        id = UUID.fromString(id),
                        name = name,
                        publicKey = publicKey,
                        lastSeen = lastSeen,
                        status = status,
                        avatar = avatar
                )

        companion object {
                /**
                         * Converts a Contact domain model into a ContactEntity for database storage.
                         *
                         * Transforms the Contact's UUID id to a String and maps all other fields directly.
                         *
                         * @param contact The Contact to convert.
                         * @return A ContactEntity representing the provided Contact.
                         */
                fun fromContact(contact: Contact): ContactEntity =
                        ContactEntity(
                                id = contact.id.toString(),
                                name = contact.name,
                                publicKey = contact.publicKey,
                                lastSeen = contact.lastSeen,
                                status = contact.status,
                                avatar = contact.avatar
                        )
        }
}
