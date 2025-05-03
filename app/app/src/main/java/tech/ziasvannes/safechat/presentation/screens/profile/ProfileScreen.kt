package tech.ziasvannes.safechat.presentation.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.ziasvannes.safechat.presentation.components.CustomTextField
import tech.ziasvannes.safechat.presentation.components.LoadingDialog
import tech.ziasvannes.safechat.presentation.preview.PreviewProfileViewModel
import tech.ziasvannes.safechat.presentation.theme.SafeChatTheme
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

/**
 * Displays the user profile screen with editable profile information and security key management.
 *
 * Shows the user's avatar, username (with edit capability), and a security section for viewing, copying, and regenerating the public key. Handles loading and error states with dialogs and snackbars. Allows toggling between view and edit modes, copying the public key to the clipboard, and generating a new key pair, which invalidates existing encrypted conversations.
 *
 * @param onNavigateBack Callback invoked when the user requests to navigate back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error snackbar if there's an error
    LaunchedEffect(state.error) {
        state.error?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
                viewModel.onEvent(ProfileEvent.ClearError)
            }
        }
    }
    
    // Display loading dialog when needed
    LoadingDialog(isLoading = state.isLoading, message = "Updating profile...")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    if (state.isEditMode) {
                        IconButton(onClick = { viewModel.onEvent(ProfileEvent.SaveProfile) }) {
                            Icon(
                                imageVector = Icons.Default.Clear, // TODO Save
                                contentDescription = "Save profile"
                            )
                        }
                    } else {
                        IconButton(onClick = { viewModel.onEvent(ProfileEvent.ToggleEditMode) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit profile"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile avatar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable(enabled = state.isEditMode) {
                        // In a real app, this would launch a photo picker
                        // For now, we'll just show a toast
                        Toast.makeText(context, "Photo picker would appear here", Toast.LENGTH_SHORT).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                // If we have an avatar URL, we would load it here with a library like Coil
                // For now, just display an icon
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile avatar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp)
                )
                
                // Edit badge (when in edit mode)
                if (state.isEditMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit avatar",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Username
            if (state.isEditMode) {
                CustomTextField(
                    value = state.userName,
                    onValueChange = { viewModel.onEvent(ProfileEvent.OnUserNameChanged(it)) },
                    label = "Name",
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = state.userName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Security section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Section title
                    Text(
                        text = "Security",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Public key info
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your Public Key",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            IconButton(onClick = { viewModel.onEvent(ProfileEvent.ToggleKeyVisibility) }) {
                                Icon(
                                    imageVector = if (state.isKeyVisible) Icons.Default.FavoriteBorder else Icons.Default.Favorite, // TODO VisibilityOff, Visibility
                                    contentDescription = if (state.isKeyVisible) "Hide key" else "Show key"
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Public key display
                        if (state.isKeyVisible) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = state.userPublicKey,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(state.userPublicKey))
                                            Toast.makeText(context, "Public key copied to clipboard", Toast.LENGTH_SHORT).show()
                                            viewModel.onEvent(ProfileEvent.CopyPublicKey)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add, // TODO ContentCopy
                                            contentDescription = "Copy public key"
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "••••••••••••••••••••••",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Generate new key pair button
                        Button(
                            onClick = {
                                // Show confirmation dialog before generating new keys
                                // For now, just generate directly
                                viewModel.onEvent(ProfileEvent.GenerateNewKeyPair)
                                Toast.makeText(context, "New key pair generated", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate New Key Pair")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Warning about generating new keys
                        Text(
                            text = "Warning: Generating a new key pair will invalidate all existing encrypted conversations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Displays a preview of the ProfileScreen composable with mock data and theming for design-time inspection.
 */
@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    SafeChatTheme {
        Surface {
            ProfileScreen(
                onNavigateBack = {},
                viewModel = PreviewProfileViewModel()
            )
        }
    }
}