package tech.ziasvannes.safechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import tech.ziasvannes.safechat.presentation.chat.ChatScreen
import tech.ziasvannes.safechat.ui.theme.SafeChatTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /**
     * Initializes the activity, sets up edge-to-edge display, and configures the main UI using Jetpack Compose.
     *
     * Applies the app theme and displays the chat interface within a scaffold layout.
     *
     * @param savedInstanceState The previously saved instance state, or null if none exists.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafeChatTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    ChatScreen(modifier = Modifier.padding(paddingValues))
                }
            }
        }
    }
}