package tech.ziasvannes.safechat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.data.models.ContactStatus
import tech.ziasvannes.safechat.presentation.theme.SafeChatColors
import tech.ziasvannes.safechat.presentation.theme.SafeChatTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * A composable that displays a contact item in the contacts list.
 *
 * @param contact The contact to display
 * @param onContactClicked Callback when this contact is clicked
 * @param modifier Optional modifier for styling
 */
/**
 * Displays a contact item row with avatar, status indicator, name, status text, and last seen time.
 *
 * The row is clickable and invokes the provided callback with the contact when selected. If the contact has an avatar URL, it is intended to display the avatar image (currently a placeholder icon is shown). The contact's status is indicated by a colored dot overlaid on the avatar, and the last seen time is formatted for readability.
 *
 * @param contact The contact to display.
 * @param onContactClicked Callback invoked when the contact row is clicked.
 * @param modifier Optional modifier for styling or layout adjustments.
 */
@Composable
fun ContactListItem(
    contact: Contact,
    onContactClicked: (Contact) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onContactClicked(contact) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with status indicator
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // If avatar URL is available, load it here
            if (contact.avatarUrl != null) {
                // Future implementation with image loading library like Coil
                // For now, just display the placeholder
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar for ${contact.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                // Display placeholder avatar
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar for ${contact.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(getStatusColor(contact.status))
                    .align(Alignment.BottomEnd)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Contact info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = getStatusText(contact.status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Last seen time
        Text(
            text = formatLastSeen(contact.lastSeen),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Maps a contact's status to its corresponding indicator color.
 *
 * @param status The contact's current status.
 * @return The color representing the given status.
 */
private fun getStatusColor(status: ContactStatus): Color {
    return when (status) {
        ContactStatus.ONLINE -> SafeChatColors.Online
        ContactStatus.AWAY -> SafeChatColors.Away
        ContactStatus.OFFLINE -> SafeChatColors.Offline
    }
}

/**
 * Returns the display label for a contact's status.
 *
 * Converts a ContactStatus value to its corresponding string: "Online", "Away", or "Offline".
 *
 * @param status The contact's current status.
 * @return The status label as a string.
 */
private fun getStatusText(status: ContactStatus): String {
    return when (status) {
        ContactStatus.ONLINE -> "Online"
        ContactStatus.AWAY -> "Away"
        ContactStatus.OFFLINE -> "Offline"
    }
}

/**
 * Converts a timestamp to a human-readable string indicating how long ago the event occurred.
 *
 * Returns "Just now" for less than a minute ago, "X min ago" for less than an hour, the time of day for events within the last 24 hours, or a date string for older timestamps.
 *
 * @param timestamp The time in milliseconds since epoch to format.
 * @return A formatted string representing the elapsed time since the given timestamp.
 */
private fun formatLastSeen(timestamp: Long): String {
    val currentTime = System.currentTimeMillis()
    val diff = currentTime - timestamp
    
    return when {
        // Less than a minute ago
        diff < 60_000 -> "Just now"
        // Less than an hour ago
        diff < 3_600_000 -> "${diff / 60_000} min ago"
        // Less than 24 hours ago
        diff < 86_400_000 -> {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
        // Otherwise show the date
        else -> {
            val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

/**
 * Displays a preview of the ContactListItem composable with sample contacts in different statuses.
 *
 * Shows three example contacts—online, away, and offline—demonstrating how the ContactListItem appears with varying names, statuses, and last seen times.
 */
@Preview(showBackground = true)
@Composable
fun ContactListItemPreview() {
    SafeChatTheme {
        Surface {
            Column {
                // Online contact
                val onlineContact = Contact(
                    id = UUID.randomUUID(),
                    name = "Jane Smith",
                    publicKey = "dummy_public_key",
                    lastSeen = System.currentTimeMillis(),
                    status = ContactStatus.ONLINE,
                    avatarUrl = null
                )
                ContactListItem(
                    contact = onlineContact,
                    onContactClicked = {}
                )
                
                // Away contact
                val awayContact = Contact(
                    id = UUID.randomUUID(),
                    name = "John Doe",
                    publicKey = "dummy_public_key",
                    lastSeen = System.currentTimeMillis() - 15 * 60 * 1000, // 15 minutes ago
                    status = ContactStatus.AWAY,
                    avatarUrl = null
                )
                ContactListItem(
                    contact = awayContact,
                    onContactClicked = {}
                )
                
                // Offline contact
                val offlineContact = Contact(
                    id = UUID.randomUUID(),
                    name = "Alex Johnson",
                    publicKey = "dummy_public_key",
                    lastSeen = System.currentTimeMillis() - 24 * 60 * 60 * 1000, // 1 day ago
                    status = ContactStatus.OFFLINE,
                    avatarUrl = null
                )
                ContactListItem(
                    contact = offlineContact,
                    onContactClicked = {}
                )
            }
        }
    }
}