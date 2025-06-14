package tech.ziasvannes.safechat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.data.models.MessageType
import tech.ziasvannes.safechat.presentation.theme.SafeChatTheme

/**
 * A composable that displays a message bubble in the chat interface.
 *
 * @param message The message to display
 * @param isFromCurrentUser Whether the message was sent by the current user
 * @param modifier Optional Modifier for styling
 */
/**
 * Displays a chat message bubble with styling and alignment based on the sender and message type.
 *
 * The bubble adapts its background color, text color, shape, and alignment depending on whether the
 * message is from the current user. Supports rendering text, image (placeholder), and file
 * messages, and shows a formatted timestamp below the bubble. If the message is from the current
 * user, a status indicator is also displayed.
 *
 * @param message The message data to display.
 * @param isFromCurrentUser Whether the message was sent by the current user.
 * @param modifier Optional modifier for layout customization.
 */
@Composable
fun MessageBubble(message: Message, isFromCurrentUser: Boolean, modifier: Modifier = Modifier) {
    val backgroundColor =
            if (isFromCurrentUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            }

    val textColor =
            if (isFromCurrentUser) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }



    val bubbleShape =
            if (isFromCurrentUser) {
                RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
            } else {
                RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
            }

    Column(
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    ) {
        Card(
                modifier = Modifier.widthIn(min = 48.dp, max = 280.dp),
                shape = bubbleShape,
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
            when (message.type) {
                is MessageType.Text -> {
                    Text(
                            text = message.content ?: "", 
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
                is MessageType.Image -> {
                    Text(
                            text = "📷 Image message (not yet implemented)",
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    )
                }
                is MessageType.File -> {
                    val fileInfo = message.type
                    Column {
                        Text(
                                text = "📎 ${fileInfo.name}",
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                                text = formatFileSize(fileInfo.size),
                                color = textColor.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                        if (isFromCurrentUser) Arrangement.End else Arrangement.Start
        ) {
            if (isFromCurrentUser) {
                Text(
                        text = getStatusIndicator(message.status),
                        style = MaterialTheme.typography.labelSmall,
                        color = getStatusColor(message.status)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                    text = formatMessageTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Converts a timestamp in milliseconds to a formatted time string (e.g., "3:45 PM").
 *
 * @param timestamp The time in milliseconds since epoch.
 * @return The formatted time string in the device's locale.
 */
private fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Converts a file size in bytes to a human-readable string in B, KB, or MB.
 *
 * @param size The file size in bytes.
 * @return The formatted file size as a string (e.g., "512 B", "2 KB", "1.5 MB").
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
 * Returns a user-friendly status string corresponding to the given message status.
 *
 * @param status The status of the message.
 * @return A string representing the message's delivery state (e.g., "Sent", "Delivered").
 */
fun getStatusIndicator(status: MessageStatus): String {
    return when (status) {
        MessageStatus.SENDING -> "Sending..."
        MessageStatus.SENT -> "Sent"
        MessageStatus.DELIVERED -> "Delivered"
        MessageStatus.READ -> "Read"
        MessageStatus.FAILED -> "Failed"
    }
}

/**
 * Returns the color associated with a given message status.
 *
 * Maps each `MessageStatus` to a specific color for use in status indicators.
 *
 * @param status The message status to map.
 * @return The color corresponding to the provided status.
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

/**
 * Displays a preview of the MessageBubble composable with sample sent, received, and file messages.
 *
 * Renders three example message bubbles inside a themed column to demonstrate different message
 * types and sender alignments.
 */
@Preview(showBackground = true)
@Composable
fun MessageBubblePreview() {
    SafeChatTheme {
        Column {
            // Sent message
            val sentMessage =
                    Message(
                            id = UUID.randomUUID(),
                            content =
                                    "Hello! This is a test message that's a bit longer to see how it wraps.",
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
            val receivedMessage =
                    Message(
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
            val fileMessage =
                    Message(
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
