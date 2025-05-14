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
        val avatarUrl: String?
) {
    /**
     * Converts this ContactEntity to a Contact domain model.
     *
     * @return A Contact object with fields mapped from this entity, converting the string ID to a
     * UUID.
     */
    fun toContact(): Contact =
            Contact(
                    id = UUID.fromString(id),
                    name = name,
                    publicKey = publicKey,
                    lastSeen = lastSeen,
                    status = status,
                    avatarUrl = avatarUrl
            )

    companion object {
        /**
         * Creates a ContactEntity from a Contact domain model.
         *
         * Converts the Contact's UUID id to a string and copies all other fields to construct a
         * ContactEntity for database storage.
         *
         * @param contact The Contact domain model to convert.
         * @return A ContactEntity representing the given Contact.
         */
        fun fromContact(contact: Contact): ContactEntity =
                ContactEntity(
                        id = contact.id.toString(),
                        name = contact.name,
                        publicKey = contact.publicKey,
                        lastSeen = contact.lastSeen,
                        status = contact.status,
                        avatarUrl = contact.avatarUrl
                )
    }
}
