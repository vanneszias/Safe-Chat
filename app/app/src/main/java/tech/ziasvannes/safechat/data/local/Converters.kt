package tech.ziasvannes.safechat.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import tech.ziasvannes.safechat.data.models.ContactStatus
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.data.models.MessageType

class Converters {
    private val gson = Gson()

    /**
 * Converts a ByteArray into a comma-separated string of decimal byte values for database storage.
 *
 * Each byte in the array is represented by its decimal value, separated by commas.
 *
 * @param value The ByteArray to be converted.
 * @return A string containing the decimal values of the bytes, separated by commas.
 */
    @TypeConverter fun fromByteArray(value: ByteArray): String = value.joinToString(",")

    /**
     * Converts a comma-separated string of byte values into a ByteArray.
     *
     * The input string should contain decimal byte values separated by commas (e.g., "1,2,3").
     *
     * @param value Comma-separated decimal byte values.
     * @return ByteArray represented by the input string.
     */
    @TypeConverter
    fun toByteArray(value: String): ByteArray = value.split(",").map { it.toByte() }.toByteArray()

    /**
             * Encodes a MessageType instance as a string for database storage.
             *
             * Converts MessageType.Text to "text", MessageType.Image to "image:&lt;url&gt;", and MessageType.File to "file:&lt;url&gt;:&lt;name&gt;:&lt;size&gt;".
             *
             * @return A string representation of the MessageType suitable for persistence.
             */
    @TypeConverter
    fun fromMessageType(type: MessageType): String =
            when (type) {
                is MessageType.Text -> "text"
                is MessageType.Image -> "image:${type.url}"
                is MessageType.File -> "file:${type.url}:${type.name}:${type.size}"
            }

    /**
             * Parses a string representation to reconstruct a MessageType object.
             *
             * Recognizes "text" for MessageType.Text, "image:&lt;url&gt;" for MessageType.Image, and
             * "file:&lt;url&gt;:&lt;name&gt;:&lt;size&gt;" for MessageType.File. If the format is unrecognized or invalid,
             * returns MessageType.Text as a fallback.
             *
             * @param value The encoded string representing a MessageType.
             * @return The corresponding MessageType instance.
             */
    @TypeConverter
    fun toMessageType(value: String): MessageType =
            when {
                value == "text" -> MessageType.Text
                value.startsWith("image:") -> MessageType.Image(value.removePrefix("image:"))
                value.startsWith("file:") -> {
                    val parts = value.removePrefix("file:").split(":")
                    if (parts.size == 3) {
                        val (url, name, sizeStr) = parts
                        MessageType.File(url, name, sizeStr.toLongOrNull() ?: 0L)
                    } else {
                        MessageType.Text // fallback
                    }
                }
                else -> MessageType.Text
            }

    /**
 * Converts a ContactStatus enum to its string name for storage in the database.
 *
 * @return The string representation of the ContactStatus.
 */
    @TypeConverter fun fromContactStatus(value: ContactStatus): String = value.name

    /**
 * Converts a string to its corresponding [ContactStatus] enum value.
 *
 * @param value The name of the contact status.
 * @return The matching [ContactStatus] enum.
 */
    @TypeConverter fun toContactStatus(value: String): ContactStatus = ContactStatus.valueOf(value)

    /**
 * Converts a MessageStatus enum to its string representation for database storage.
 *
 * @return The name of the MessageStatus enum.
 */
    @TypeConverter fun fromMessageStatus(value: MessageStatus): String = value.name

    /**
 * Converts a string to its corresponding `MessageStatus` enum value.
 *
 * @param value The name of the `MessageStatus` enum constant.
 * @return The matching `MessageStatus` enum.
 */
    @TypeConverter fun toMessageStatus(value: String): MessageStatus = MessageStatus.valueOf(value)
}
