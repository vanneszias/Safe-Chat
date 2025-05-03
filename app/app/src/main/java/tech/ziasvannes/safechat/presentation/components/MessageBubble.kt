package tech.ziasvannes.safechat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.data.models.MessageType
import tech.ziasvannes.safechat.presentation.theme.SafeChatTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * A composable that displays a message bubble in the chat interface.
 *
 * @param message The message to display
 * @param isFromCurrentUser Whether the message was sent by the current user
 * @param modifier Optional Modifier for styling
 */
@Composable
fun MessageBubble(
    message: Message,
    isFromCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isFromCurrentUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isFromCurrentUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val alignment = if (isFromCurrentUser) {
        Alignment.CenterEnd
    } else {
        Alignment.CenterStart
    }

    val bubbleShape = if (isFromCurrentUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(backgroundColor)
                .padding(12.dp)
        ) {
            when (message.type) {
                is MessageType.Text -> {
                    Text(
                        text = message.content,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is MessageType.Image -> {
                    // Image display logic will be implemented here
                    Text(
                        text = "Image message (not yet implemented)",
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is MessageType.File -> {
                    // File message display logic will be implemented here
                    val fileInfo = message.type as MessageType.File
                    Text(
                        text = "File: ${fileInfo.name} (${formatFileSize(fileInfo.size)})",
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // Time and status indicators
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatMessageTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            if (isFromCurrentUser) {
                Text(
                    text = getStatusIndicator(message.status),
                    style = MaterialTheme.typography.labelSmall,
                    color = getStatusColor(message.status)
                )
            }
        }
    }
}

/**
 * Formats a timestamp into a readable time string
 */
private fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Formats a file size from bytes to a human-readable string
 */
private fun formatFileSize(size: Long): String {
    val kb = 1024
    val mb = kb * 1024
    
    return when {
        size < kb -> "$size B"
        size < mb -> "${size / kb} KB"
        else -> String.format("%.1f MB", size.toFloat() / mb)
    }
}

/**
 * Returns a string indicator based on message status
 */
private fun getStatusIndicator(status: MessageStatus): String {
    return when (status) {
        MessageStatus.SENDING -> "Sending..."
        MessageStatus.SENT -> "Sent"
        MessageStatus.DELIVERED -> "Delivered"
        MessageStatus.READ -> "Read"
        MessageStatus.FAILED -> "Failed"
    }
}

/**
 * Returns a color based on message status
 */
private fun getStatusColor(status: MessageStatus): Color {
    return when (status) {
        MessageStatus.SENDING -> Color.Gray
        MessageStatus.SENT -> Color.Gray
        MessageStatus.DELIVERED -> Color(0xFF2196F3)
        MessageStatus.READ -> Color(0xFF4CAF50)
        MessageStatus.FAILED -> Color(0xFFB00020)
    }
}

@Preview(showBackground = true)
@Composable
fun MessageBubblePreview() {
    SafeChatTheme {
        Column {
            // Sent message
            val sentMessage = Message(
                id = UUID.randomUUID(),
                content = "Hello! This is a test message that's a bit longer to see how it wraps.",
                timestamp = System.currentTimeMillis(),
                senderId = UUID.randomUUID(),
                receiverId = UUID.randomUUID(),
                status = MessageStatus.READ,
                type = MessageType.Text,
                encryptedContent = ByteArray(0),
                iv = ByteArray(0)
            )
            MessageBubble(message = sentMessage, isFromCurrentUser = true)
            
            // Received message
            val receivedMessage = Message(
                id = UUID.randomUUID(),
                content = "Hi there! This is a response message.",
                timestamp = System.currentTimeMillis(),
                senderId = UUID.randomUUID(),
                receiverId = UUID.randomUUID(),
                status = MessageStatus.DELIVERED,
                type = MessageType.Text,
                encryptedContent = ByteArray(0),
                iv = ByteArray(0)
            )
            MessageBubble(message = receivedMessage, isFromCurrentUser = false)
            
            // File message
            val fileMessage = Message(
                id = UUID.randomUUID(),
                content = "Document.pdf",
                timestamp = System.currentTimeMillis(),
                senderId = UUID.randomUUID(),
                receiverId = UUID.randomUUID(),
                status = MessageStatus.SENT,
                type = MessageType.File("", "Document.pdf", 1024 * 1024 * 2),
                encryptedContent = ByteArray(0),
                iv = ByteArray(0)
            )
            MessageBubble(message = fileMessage, isFromCurrentUser = true)
        }
    }
}