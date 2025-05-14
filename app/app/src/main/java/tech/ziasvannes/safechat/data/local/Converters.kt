package tech.ziasvannes.safechat.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import tech.ziasvannes.safechat.data.models.ContactStatus
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.data.models.MessageType

class Converters {
    private val gson = Gson()

    /**
     * Converts a ByteArray to a comma-separated String for database storage.
     *
     * Each byte is represented as its decimal value, separated by commas.
     *
     * @param value The ByteArray to convert.
     * @return A comma-separated String representing the ByteArray.
     */
    @TypeConverter fun fromByteArray(value: ByteArray): String = value.joinToString(",")

    /**
     * Converts a comma-separated string of byte values into a ByteArray.
     *
     * @param value A string containing byte values separated by commas (e.g., "1,2,3").
     * @return The corresponding ByteArray.
     */
    @TypeConverter
    fun toByteArray(value: String): ByteArray = value.split(",").map { it.toByte() }.toByteArray()

    /**
     * Converts a MessageType object to its JSON string representation for database storage.
     *
     * @param value The MessageType instance to serialize.
     * @return A JSON string representing the MessageType.
     */
    @TypeConverter fun fromMessageType(value: MessageType): String = gson.toJson(value)

    /**
     * Deserializes a JSON string into a MessageType object.
     *
     * @param value JSON string representing a MessageType.
     * @return The corresponding MessageType object.
     */
    @TypeConverter
    fun toMessageType(value: String): MessageType = gson.fromJson(value, MessageType::class.java)

    /**
     * Converts a [ContactStatus] enum to its string name for database storage.
     *
     * @return The name of the [ContactStatus] as a string.
     */
    @TypeConverter fun fromContactStatus(value: ContactStatus): String = value.name

    /**
     * Converts a string representation of a contact status to its corresponding [ContactStatus]
     * enum value.
     *
     * @param value The name of the contact status.
     * @return The [ContactStatus] enum matching the provided name.
     */
    @TypeConverter fun toContactStatus(value: String): ContactStatus = ContactStatus.valueOf(value)

    /**
     * Converts a MessageStatus enum to its string name for database storage.
     *
     * @return The name of the MessageStatus enum as a String.
     */
    @TypeConverter fun fromMessageStatus(value: MessageStatus): String = value.name

    /**
     * Converts a string representation of a message status to its corresponding MessageStatus enum.
     *
     * @param value The name of the MessageStatus enum as a string.
     * @return The MessageStatus enum matching the provided string.
     */
    @TypeConverter fun toMessageStatus(value: String): MessageStatus = MessageStatus.valueOf(value)
}
