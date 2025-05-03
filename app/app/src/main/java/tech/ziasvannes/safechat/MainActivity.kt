package tech.ziasvannes.safechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import dagger.hilt.android.AndroidEntryPoint
import tech.ziasvannes.safechat.presentation.SafeChatApp
import tech.ziasvannes.safechat.presentation.theme.SafeChatTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /**
     * Initializes the activity, sets up edge-to-edge display, and composes the main app UI using Jetpack Compose.
     *
     * @param savedInstanceState The previously saved instance state, or null if none exists.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafeChatTheme {
                SafeChatApp()
            }
        }
    }
}