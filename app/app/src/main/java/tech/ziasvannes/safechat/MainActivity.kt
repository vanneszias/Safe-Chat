package tech.ziasvannes.safechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import tech.ziasvannes.safechat.ui.theme.SafeChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafeChatTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    ChatScreen(modifier = Modifier.padding(it))
                }
            }
        }
    }
}


@Composable
fun ChatScreen(
    modifier: Modifier = Modifier
) {
    // This will be the chat screen, it will be a contact card, a list of messages, and a text field to send messages

}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    SafeChatTheme {
        ChatScreen()
    }
}