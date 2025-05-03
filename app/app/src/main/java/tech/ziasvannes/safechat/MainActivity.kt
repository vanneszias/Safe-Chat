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
     * Initializes the activity, enables edge-to-edge display, and sets the main app UI with Jetpack Compose.
     *
     * @param savedInstanceState The previously saved instance state, or null if the activity is newly created.
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