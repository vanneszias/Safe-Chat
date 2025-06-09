package tech.ziasvannes.safechat.presentation.screens.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.extended.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.ziasvannes.safechat.presentation.components.LoadingDialog
import tech.ziasvannes.safechat.presentation.theme.SafeChatTheme

/**
 * Displays the main settings screen with configurable options grouped into sections.
 *
 * Presents appearance, notification, privacy, and about settings, allowing users to toggle
 * features, view app information, and perform actions such as clearing messages or resetting
 * settings. Handles user interactions, confirmation dialogs for destructive actions, loading and
 * error feedback, and navigation.
 *
 * @param onNavigateBack Callback invoked when the user requests to navigate back from the settings
 * screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
/**
 * Displays the main settings screen with configurable sections for appearance, notifications,
 * privacy, developer options, and app information.
 *
 * Presents toggles and actions for various user preferences, including dark mode, notifications,
 * message retention, database encryption, and developer test settings. Supports destructive actions
 * with confirmation dialogs and displays error messages as snackbars. Navigation callbacks allow
 * returning to the previous screen and accessing test mode settings.
 *
 * @param onNavigateBack Callback invoked when the user requests to navigate back.
 */
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show confirmation dialog state
    var showClearMessagesDialog by remember { mutableStateOf(false) }
    var showResetSettingsDialog by remember { mutableStateOf(false) }

    // Show error snackbar if there's an error
    LaunchedEffect(state.error) {
        state.error?.let {
            val result =
                    snackbarHostState.showSnackbar(
                            message = it,
                            actionLabel = "Dismiss",
                            duration = SnackbarDuration.Short
                    )
            if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
                viewModel.onEvent(SettingsEvent.ClearError)
            }
        }
    }

    // Display loading dialog when needed
    LoadingDialog(isLoading = state.isLoading, message = "Updating settings...")

    // Confirmation dialogs
    if (showClearMessagesDialog) {
        AlertDialog(
                onDismissRequest = { showClearMessagesDialog = false },
                title = { Text("Clear All Messages") },
                text = {
                    Text("This will permanently delete all messages. This action cannot be undone.")
                },
                confirmButton = {
                    Button(
                            onClick = {
                                viewModel.onEvent(SettingsEvent.ClearAllMessages)
                                showClearMessagesDialog = false
                                Toast.makeText(context, "All messages cleared", Toast.LENGTH_SHORT)
                                        .show()
                            }
                    ) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearMessagesDialog = false }) { Text("Cancel") }
                }
        )
    }

    if (showResetSettingsDialog) {
        AlertDialog(
                onDismissRequest = { showResetSettingsDialog = false },
                title = { Text("Reset Settings") },
                text = { Text("This will reset all settings to their default values.") },
                confirmButton = {
                    Button(
                            onClick = {
                                viewModel.onEvent(SettingsEvent.ResetSettings)
                                showResetSettingsDialog = false
                                Toast.makeText(
                                                context,
                                                "Settings reset to defaults",
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            }
                    ) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetSettingsDialog = false }) { Text("Cancel") }
                }
        )
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Settings") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Navigate back"
                                )
                            }
                        }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(paddingValues)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
        ) {
            // Appearance section
            SettingsSection("Appearance") {
                SwitchSettingItem(
                        title = "Dark Mode",
                        description = "Use dark theme",
                        icon = Icons.Default.Settings,
                        checked = state.isDarkMode,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleDarkMode(it)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notifications section
            SettingsSection("Notifications") {
                SwitchSettingItem(
                        title = "Enable Notifications",
                        description = "Receive alerts for new messages",
                        icon = Icons.Default.Notifications,
                        checked = state.notificationsEnabled,
                        onCheckedChange = {
                            viewModel.onEvent(SettingsEvent.ToggleNotifications(it))
                        }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy & Security section
            SettingsSection("Privacy & Security") {
                // Message retention
                SettingItem(
                        title = "Message Retention",
                        description = "Keep messages for ${state.messageRetentionPeriod} days",
                        icon = Icons.Default.DateRange,
                        onClick = {
                            // In a real app, this would show a dialog to select retention period
                            Toast.makeText(
                                            context,
                                            "Select message retention period",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
                )

                SwitchSettingItem(
                        title = "Auto Delete Messages",
                        description = "Automatically delete old messages",
                        icon = Icons.Default.Clear,
                        checked = state.autoDeleteMessages,
                        onCheckedChange = {
                            viewModel.onEvent(SettingsEvent.ToggleAutoDeleteMessages(it))
                        }
                )

                SwitchSettingItem(
                        title = "Encrypt Database",
                        description = "Encrypt all locally stored data",
                        icon = Icons.Default.Lock,
                        checked = state.encryptDatabase,
                        onCheckedChange = {
                            viewModel.onEvent(SettingsEvent.ToggleDatabaseEncryption(it))
                        }
                )

                SettingItem(
                        title = "Clear All Messages",
                        description = "Permanently delete all messages",
                        icon = Icons.Default.Delete,
                        onClick = { showClearMessagesDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About section
            SettingsSection("About") {
                SettingItem(
                        title = "App Version",
                        description = "1.0.0", // TODO Version get from buildconfig
                        icon = Icons.Default.Info,
                        onClick = {}
                )

                SettingItem(
                        title = "Privacy Policy",
                        description = "View our privacy policy",
                        icon = Icons.Default.Lock,
                        onClick = {
                            Toast.makeText(context, "View privacy policy", Toast.LENGTH_SHORT)
                                    .show()
                        }
                )

                SettingItem(
                        title = "Terms of Service",
                        description = "View our terms of service",
                        icon = Icons.Default.Info,
                        onClick = {
                            Toast.makeText(context, "View terms of service", Toast.LENGTH_SHORT)
                                    .show()
                        }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Reset settings button
            Button(
                    onClick = { showResetSettingsDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset to Default Settings")
            }
        }
    }
}

/**
 * Displays a titled section within the settings screen, wrapping the provided content in a card.
 *
 * @param title The header text for the section.
 * @param content The composable content to display within the section.
 */
@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
        )

        Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) { Column(modifier = Modifier.fillMaxWidth()) { content() } }
    }
}

/**
 * Displays a clickable settings row with an icon, title, description, and a trailing icon.
 *
 * @param title The title text of the setting.
 * @param description The description or summary of the setting.
 * @param icon The icon displayed at the start of the row.
 * @param onClick Callback invoked when the row is clicked.
 */
@Composable
fun SettingItem(
        title: String,
        description: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit
) {
    Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            color = Color.Transparent
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)

                Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

/**
 * Displays a settings row with an icon, title, description, and a toggle switch.
 *
 * @param title The title of the setting.
 * @param description A brief description of the setting.
 * @param icon The icon representing the setting.
 * @param checked Whether the switch is on or off.
 * @param onCheckedChange Callback invoked when the switch state changes.
 */
@Composable
fun SwitchSettingItem(
        title: String,
        description: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)

                Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

/**
 * Displays a design-time preview of the Settings screen with sample settings state.
 *
 * Uses a fake ViewModel and preset values to render the SettingsScreen composable for UI inspection
 * in the IDE.
 */
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    val state =
            SettingsState(
                    isDarkMode = true,
                    notificationsEnabled = true,
                    messageRetentionPeriod = 30,
                    autoDeleteMessages = false,
                    encryptDatabase = true,
                    isLoading = false
            )

    SafeChatTheme {
        Surface { SettingsScreen(onNavigateBack = {}, viewModel = FakeSettingsViewModel(state)) }
    }
}

// Fake ViewModel for preview
private class FakeSettingsViewModel(initialState: SettingsState) : SettingsViewModel() {
    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<SettingsState> = _state.asStateFlow()

    /**
     * Handles settings events. No operation is performed in the preview implementation.
     *
     * @param event The settings event to handle.
     */
    override fun onEvent(event: SettingsEvent) {
        // No-op for preview
    }
}
