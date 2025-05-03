package tech.ziasvannes.safechat.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import tech.ziasvannes.safechat.data.models.ContactStatus
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.data.models.MessageType

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromByteArray(value: ByteArray): String = value.joinToString(",")
    
    @TypeConverter
    fun toByteArray(value: String): ByteArray = value.split(",").map { it.toByte() }.toByteArray()
    
    @TypeConverter
    fun fromMessageType(value: MessageType): String = gson.toJson(value)
    
    @TypeConverter
    fun toMessageType(value: String): MessageType = gson.fromJson(value, MessageType::class.java)
    
    @TypeConverter
    fun fromContactStatus(value: ContactStatus): String = value.name
    
    @TypeConverter
    fun toContactStatus(value: String): ContactStatus = ContactStatus.valueOf(value)
    
    @TypeConverter
    fun fromMessageStatus(value: MessageStatus): String = value.name
    
    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus = MessageStatus.valueOf(value)
}