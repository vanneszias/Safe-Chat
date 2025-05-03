package tech.ziasvannes.safechat.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tech.ziasvannes.safechat.data.models.EncryptionStatus
import tech.ziasvannes.safechat.presentation.theme.SafeChatColors
import tech.ziasvannes.safechat.presentation.theme.SafeChatTheme

/**
 * A composable that displays the encryption status of a chat session.
 *
 * @param status The current encryption status
 * @param modifier Optional modifier for styling
 */
/**
 * Displays an animated indicator representing the current encryption status of a chat session.
 *
 * Visually reflects the encryption state by animating background and icon colors, and applies a continuous rotation to the icon when encryption setup is in progress. The indicator shows an icon and a descriptive label corresponding to the provided status.
 *
 * @param status The current encryption status to display.
 * @param modifier Optional modifier for styling and layout customization.
 */
@Composable
fun EncryptionStatusIndicator(
    status: EncryptionStatus,
    modifier: Modifier = Modifier
) {
    val backgroundColor = animateColorAsState(
        targetValue = when (status) {
            EncryptionStatus.ENCRYPTED -> SafeChatColors.Encrypted.copy(alpha = 0.2f)
            EncryptionStatus.NOT_ENCRYPTED -> SafeChatColors.NotEncrypted.copy(alpha = 0.2f)
            EncryptionStatus.KEY_EXCHANGE_IN_PROGRESS -> SafeChatColors.KeyExchangeInProgress.copy(alpha = 0.2f)
        },
        animationSpec = tween(300),
        label = "Background Color Animation"
    )
    
    val contentColor = animateColorAsState(
        targetValue = when (status) {
            EncryptionStatus.ENCRYPTED -> SafeChatColors.Encrypted
            EncryptionStatus.NOT_ENCRYPTED -> SafeChatColors.NotEncrypted
            EncryptionStatus.KEY_EXCHANGE_IN_PROGRESS -> SafeChatColors.KeyExchangeInProgress
        },
        animationSpec = tween(300),
        label = "Content Color Animation"
    )

    // For the KEY_EXCHANGE_IN_PROGRESS state, we'll add a rotation animation
    var rotationAngle by remember { mutableStateOf(0f) }
    val rotation = animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = tween(500),
        label = "Rotation Animation"
    )
    
    // Update the rotation angle when in KEY_EXCHANGE_IN_PROGRESS state
    LaunchedEffect(status) {
        if (status == EncryptionStatus.KEY_EXCHANGE_IN_PROGRESS) {
            while (true) {
                rotationAngle = (rotationAngle + 60f) % 360f
                delay(500)
            }
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor.value)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Icon based on encryption status
        Icon(
            imageVector = when (status) {
                EncryptionStatus.ENCRYPTED -> Icons.Default.Lock
                EncryptionStatus.NOT_ENCRYPTED -> Icons.Default.Clear  // TODO Lock Open
                EncryptionStatus.KEY_EXCHANGE_IN_PROGRESS -> Icons.Default.Check // TODO Sync
            },
            contentDescription = "Encryption Status",
            tint = contentColor.value,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer {
                    if (status == EncryptionStatus.KEY_EXCHANGE_IN_PROGRESS) {
                        rotationZ = rotation.value
                    }
                }
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Status text
        Text(
            text = when (status) {
                EncryptionStatus.ENCRYPTED -> "Encrypted"
                EncryptionStatus.NOT_ENCRYPTED -> "Not Encrypted"
                EncryptionStatus.KEY_EXCHANGE_IN_PROGRESS -> "Setting up encryption..."
            },
            style = MaterialTheme.typography.labelMedium,
            color = contentColor.value
        )
    }
}

/**
 * Displays a preview of the EncryptionStatusIndicator composable in all encryption states.
 *
 * Shows the indicator for ENCRYPTED, NOT_ENCRYPTED, and KEY_EXCHANGE_IN_PROGRESS statuses within a themed surface for design-time inspection.
 */
@Preview(showBackground = true)
@Composable
fun EncryptionStatusIndicatorPreview() {
    SafeChatTheme {
        Surface {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Encrypted status
                EncryptionStatusIndicator(status = EncryptionStatus.ENCRYPTED)
                
                // Not encrypted status
                EncryptionStatusIndicator(status = EncryptionStatus.NOT_ENCRYPTED)
                
                // Key exchange in progress status
                EncryptionStatusIndicator(status = EncryptionStatus.KEY_EXCHANGE_IN_PROGRESS)
            }
        }
    }
}