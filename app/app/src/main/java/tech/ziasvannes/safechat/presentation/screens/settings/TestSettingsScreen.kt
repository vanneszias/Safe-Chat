package tech.ziasvannes.safechat.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.ziasvannes.safechat.testing.TestMode

/**
 * A screen that allows control of test mode settings for the Safe Chat app.
 * 
 * This screen provides UI controls to toggle between real and test repositories,
 * enable simulation of incoming messages, and other test features.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestSettingsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Mode Settings") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            // Use test repositories setting
            var useTestRepositories by remember { mutableStateOf(TestMode.useTestRepositories) }
            var simulateIncomingMessages by remember { mutableStateOf(TestMode.simulateIncomingMessages) }
            var simulateConnectionIssues by remember { mutableStateOf(TestMode.simulateConnectionIssues) }
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Test Data Settings",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Use test repositories toggle
                    SettingToggle(
                        title = "Use Test Repositories",
                        description = "Enable to use fake repositories with pre-populated test data",
                        checked = useTestRepositories,
                        onCheckedChange = { checked ->
                            useTestRepositories = checked
                            TestMode.useTestRepositories = checked
                        }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Simulate incoming messages toggle
                    SettingToggle(
                        title = "Simulate Incoming Messages",
                        description = "Periodically generate random incoming messages to test notifications and UI updates",
                        checked = simulateIncomingMessages,
                        onCheckedChange = { checked ->
                            simulateIncomingMessages = checked
                            TestMode.simulateIncomingMessages = checked
                        },
                        enabled = useTestRepositories
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Simulate connection issues toggle
                    SettingToggle(
                        title = "Simulate Connection Issues",
                        description = "Randomly simulate network latency and connectivity problems",
                        checked = simulateConnectionIssues,
                        onCheckedChange = { checked ->
                            simulateConnectionIssues = checked
                            TestMode.simulateConnectionIssues = checked
                        },
                        enabled = useTestRepositories
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Note: Changes may require restarting parts of the app to take full effect.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}