package tech.ziasvannes.safechat.presentation.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.data.models.ContactStatus
import tech.ziasvannes.safechat.presentation.theme.SafeChatColors
import tech.ziasvannes.safechat.presentation.theme.SafeChatTheme

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
 * The row is clickable and invokes the provided callback with the contact when selected. The avatar
 * area shows a placeholder icon if no avatar URL is present, and a colored status indicator
 * reflects the contact's current status.
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
            modifier =
                    modifier.fillMaxWidth().clickable { onContactClicked(contact) }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with status indicator
        Box(
                modifier =
                        Modifier.size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
        ) {
            if (contact.avatar != null) {
                val imageBitmap =
                        try {
                            val decodedBytes = Base64.decode(contact.avatar, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                    ?.asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                if (imageBitmap != null) {
                    Image(
                            bitmap = imageBitmap,
                            contentDescription = "Avatar for ${contact.name}",
                            modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar for ${contact.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar for ${contact.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                )
            }

            // Status indicator
            Box(
                    modifier =
                            Modifier.size(12.dp)
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
 * Returns a human-readable string representing the given contact status.
 *
 * @param status The contact's status.
 * @return "Online", "Away", or "Offline" depending on the status value.
 */
private fun getStatusText(status: ContactStatus): String {
    return when (status) {
        ContactStatus.ONLINE -> "Online"
        ContactStatus.AWAY -> "Away"
        ContactStatus.OFFLINE -> "Offline"
    }
}

/**
 * Converts a timestamp to a human-readable last seen string.
 *
 * Returns "Just now" for times under a minute ago, "X min ago" for times under an hour, the time of
 * day for times within the last 24 hours, or a date string for older timestamps.
 *
 * @param timestamp The epoch time in milliseconds representing the last seen moment.
 * @return A formatted string describing how long ago the timestamp occurred.
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
 * Shows three example contacts (Online, Away, Offline) to demonstrate the appearance and behavior
 * of the contact list item UI component.
 */
@Preview(showBackground = true)
@Composable
fun ContactListItemPreview() {
    SafeChatTheme {
        Surface {
            Column {
                // Online contact
                val onlineContact =
                        Contact(
                                id = UUID.randomUUID(),
                                name = "Jane Smith",
                                publicKey = "dummy_public_key",
                                lastSeen = System.currentTimeMillis(),
                                status = ContactStatus.ONLINE,
                                avatar = null
                        )
                ContactListItem(contact = onlineContact, onContactClicked = {})

                // Away contact
                val awayContact =
                        Contact(
                                id = UUID.randomUUID(),
                                name = "John Doe",
                                publicKey = "dummy_public_key",
                                lastSeen =
                                        System.currentTimeMillis() -
                                                15 * 60 * 1000, // 15 minutes ago
                                status = ContactStatus.AWAY,
                                avatar = null
                        )
                ContactListItem(contact = awayContact, onContactClicked = {})

                // Offline contact
                val offlineContact =
                        Contact(
                                id = UUID.randomUUID(),
                                name = "Alex Johnson",
                                publicKey = "dummy_public_key",
                                lastSeen =
                                        System.currentTimeMillis() -
                                                24 * 60 * 60 * 1000, // 1 day ago
                                status = ContactStatus.OFFLINE,
                                avatar = null
                        )
                ContactListItem(contact = offlineContact, onContactClicked = {})
            }
        }
    }
}
