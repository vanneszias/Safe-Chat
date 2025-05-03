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
     * Handles activity creation, enables edge-to-edge display, and sets the main app UI with Jetpack Compose.
     *
     * @param savedInstanceState The saved state of the activity, or null if none exists.
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