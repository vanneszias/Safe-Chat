package tech.ziasvannes.safechat.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import tech.ziasvannes.safechat.presentation.theme.SafeChatTheme

/**
 * A simple animated loading indicator with optional text message.
 *
 * @param modifier Optional modifier for styling
 * @param message Optional message to display below the indicator
 * @param color The color of the loading indicator dots
 */
/**
 * Displays an animated loading indicator with three scaling dots and an optional message.
 *
 * The dots animate in a staggered wave pattern to indicate a loading state. An optional message can
 * be shown below the indicator.
 *
 * @param modifier Modifier to be applied to the indicator layout.
 * @param message Optional text displayed below the loading dots.
 * @param color Color of the animated dots.
 */
@Composable
fun LoadingIndicator(
        modifier: Modifier = Modifier,
        message: String? = null,
        color: Color = MaterialTheme.colorScheme.primary
) {
    Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        // Animation for the dots
        val infiniteTransition = rememberInfiniteTransition(label = "Loading Animation")

        // Define the animation properties for each dot
        val firstDotScale by
                infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1.0f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(600),
                                        repeatMode = RepeatMode.Reverse,
                                        initialStartOffset = StartOffset(0)
                                ),
                        label = "First Dot Scale"
                )

        val secondDotScale by
                infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1.0f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(600),
                                        repeatMode = RepeatMode.Reverse,
                                        initialStartOffset = StartOffset(200)
                                ),
                        label = "Second Dot Scale"
                )

        val thirdDotScale by
                infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1.0f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(600),
                                        repeatMode = RepeatMode.Reverse,
                                        initialStartOffset = StartOffset(400)
                                ),
                        label = "Third Dot Scale"
                )

        // Display the animated dots
        Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
        ) {
            Box(
                    modifier =
                            Modifier.size(8.dp)
                                    .scale(firstDotScale)
                                    .clip(CircleShape)
                                    .background(color)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                    modifier =
                            Modifier.size(8.dp)
                                    .scale(secondDotScale)
                                    .clip(CircleShape)
                                    .background(color)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                    modifier =
                            Modifier.size(8.dp)
                                    .scale(thirdDotScale)
                                    .clip(CircleShape)
                                    .background(color)
            )
        }

        // Display the message if provided
        if (!message.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Displays a modal dialog with a loading indicator, blocking user interaction while active.
 *
 * Shows the dialog only when [isLoading] is true. An optional [message] can be displayed below the
 * loading indicator. The dialog can be configured to allow dismissal via back press or clicking
 * outside using [dismissOnBackPress] and [dismissOnClickOutside].
 */
@Composable
fun LoadingDialog(
        isLoading: Boolean,
        message: String? = null,
        dismissOnBackPress: Boolean = false,
        dismissOnClickOutside: Boolean = false
) {
    if (isLoading) {
        Dialog(
                onDismissRequest = {},
                properties =
                        DialogProperties(
                                dismissOnBackPress = dismissOnBackPress,
                                dismissOnClickOutside = dismissOnClickOutside
                        )
        ) {
            Surface(
                    modifier = Modifier.wrapContentSize().padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
            ) { LoadingIndicator(modifier = Modifier.padding(24.dp), message = message) }
        }
    }
}

/**
 * Displays a full-screen semi-transparent overlay with a centered animated loading indicator and an
 * optional message.
 *
 * @param message The message to display below the loading indicator, or null to omit the message.
 */
@Composable
fun FullScreenLoading(message: String? = "Loading...") {
    Box(
            contentAlignment = Alignment.Center,
            modifier =
                    Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
    ) { LoadingIndicator(message = message) }
}

/**
 * Displays a preview of the LoadingIndicator composable with and without a message.
 *
 * This preview demonstrates the appearance of the loading indicator component in different
 * configurations within the app's theme.
 */
@Preview(showBackground = true)
@Composable
fun LoadingIndicatorPreview() {
    SafeChatTheme {
        Surface {
            Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Loading indicator without message
                LoadingIndicator()

                // Loading indicator with message
                LoadingIndicator(message = "Loading messages...")
            }
        }
    }
}
