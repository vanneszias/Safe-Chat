package tech.ziasvannes.safechat.presentation.screens.profile

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.extended.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.ziasvannes.safechat.presentation.components.CustomTextField
import tech.ziasvannes.safechat.presentation.components.LoadingDialog
import tech.ziasvannes.safechat.presentation.preview.PreviewProfileViewModel
import tech.ziasvannes.safechat.presentation.theme.SafeChatTheme

/**
 * Displays the user profile screen with editable profile information and security key management.
 *
 * Shows the user's avatar, username (with edit capability), and a security section for viewing,
 * copying, and regenerating the public key. Handles loading and error states with dialogs and
 * snackbars. Allows toggling between view and edit modes, copying the public key to the clipboard,
 * and generating a new key pair, which invalidates existing encrypted conversations.
 *
 * @param onNavigateBack Callback invoked when the user requests to navigate back.
 * @param onLogout Callback invoked when the user logs out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
        onNavigateBack: () -> Unit,
        onLogout: () -> Unit = {},
        viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val snackbarHostState = remember { SnackbarHostState() }

    // --- Image picker state and launchers ---
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var tempCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    // Camera launcher
    val cameraLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success && tempCameraUri != null) {
                    viewModel.onEvent(ProfileEvent.OnAvatarSelected(context, tempCameraUri!!))
                }
            }
    // Gallery launcher
    val galleryLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let { viewModel.onEvent(ProfileEvent.OnAvatarSelected(context, it)) }
            }

    // Permission launcher (for camera)
    val cameraPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted
                ->
                if (granted) {
                    val uri = createImageUri(context)
                    if (uri != null) {
                        tempCameraUri = uri
                        cameraLauncher.launch(uri)
                    }
                } else {
                    Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }

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
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Navigate back"
                                )
                            }
                        },
                        actions = {
                            if (state.isEditMode) {
                                IconButton(
                                        onClick = { viewModel.onEvent(ProfileEvent.SaveProfile) }
                                ) {
                                    Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = "Save profile"
                                    )
                                }
                            } else {
                                IconButton(
                                        onClick = { viewModel.onEvent(ProfileEvent.ToggleEditMode) }
                                ) {
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
                modifier =
                        Modifier.fillMaxSize()
                                .padding(paddingValues)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile avatar
            Box(
                    modifier =
                            Modifier.size(120.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable(enabled = state.isEditMode) {
                                        showImagePickerDialog = true
                                    },
                    contentAlignment = Alignment.Center
            ) {
                if (!state.avatar.isNullOrBlank()) {
                    val imageBitmap =
                            remember(state.avatar) {
                                try {
                                    val decodedBytes = Base64.decode(state.avatar, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(
                                                    decodedBytes,
                                                    0,
                                                    decodedBytes.size
                                            )
                                            ?.asImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            }
                    if (imageBitmap != null) {
                        Image(
                                bitmap = imageBitmap,
                                contentDescription = "Profile avatar",
                                modifier = Modifier.size(120.dp).clip(CircleShape)
                        )
                    } else {
                        Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile avatar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                        )
                    }
                } else {
                    Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile avatar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                    )
                }

                // Edit badge (when in edit mode)
                if (state.isEditMode) {
                    Box(
                            modifier =
                                    Modifier.align(Alignment.BottomEnd)
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

            // --- Image Picker Dialog ---
            if (showImagePickerDialog) {
                AlertDialog(
                        onDismissRequest = { showImagePickerDialog = false },
                        title = { Text("Change Profile Picture") },
                        text = { Text("Choose a method to set your profile picture.") },
                        confirmButton = {
                            Column {
                                Button(
                                        onClick = {
                                            showImagePickerDialog = false
                                            val cameraPermission = Manifest.permission.CAMERA
                                            if (ContextCompat.checkSelfPermission(
                                                            context,
                                                            cameraPermission
                                                    ) == PackageManager.PERMISSION_GRANTED
                                            ) {
                                                val uri = createImageUri(context)
                                                if (uri != null) {
                                                    tempCameraUri = uri
                                                    cameraLauncher.launch(uri)
                                                }
                                            } else {
                                                cameraPermissionLauncher.launch(cameraPermission)
                                            }
                                        }
                                ) {
                                    Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = null
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Take Photo")
                                }
                                Spacer(Modifier.height(8.dp))
                                Button(
                                        onClick = {
                                            showImagePickerDialog = false
                                            galleryLauncher.launch("image/*")
                                        }
                                ) {
                                    Icon(
                                            imageVector = Icons.Default.PhotoLibrary,
                                            contentDescription = null
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Choose from Gallery")
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showImagePickerDialog = false }) {
                                Text("Cancel")
                            }
                        }
                )
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
                Spacer(modifier = Modifier.height(4.dp))
                if (state.userId.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                                text = "ID: ${state.userId}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                                onClick = {
                                    clipboard.setPrimaryClip(
                                            ClipData.newPlainText("ID", state.userId)
                                    )
                                }
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Copy ID")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Security section
            Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                            )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    // Section title
                    Text(
                            text = "Security",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Public key info
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                    text = "Your Public Key",
                                    style = MaterialTheme.typography.titleMedium
                            )

                            IconButton(
                                    onClick = {
                                        viewModel.onEvent(ProfileEvent.ToggleKeyVisibility)
                                    }
                            ) {
                                Icon(
                                        imageVector =
                                                if (state.isKeyVisible) Icons.Default.Close
                                                else Icons.Default.Info,
                                        contentDescription =
                                                if (state.isKeyVisible) "Hide key" else "Show key"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Public key display
                        if (state.isKeyVisible) {
                            Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                                                clipboard.setPrimaryClip(
                                                        ClipData.newPlainText(
                                                                "Public Key",
                                                                state.userPublicKey
                                                        )
                                                )
                                            }
                                    ) {
                                        Icon(
                                                imageVector = Icons.Default.Share,
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
                                    // Show confirmation dialog before
                                    // generating new keys
                                    // For now, just generate directly
                                    viewModel.onEvent(ProfileEvent.GenerateNewKeyPair)
                                    Toast.makeText(
                                                    context,
                                                    "New key pair generated",
                                                    Toast.LENGTH_SHORT
                                            )
                                            .show()
                                },
                                modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate New Key Pair")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Warning about generating new keys
                        Text(
                                text =
                                        "Warning: Generating a new key pair will invalidate all existing encrypted conversations.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout section
            Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                            )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                            text = "Account",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Logout button
                    Button(
                            onClick = {
                                viewModel.onEvent(ProfileEvent.Logout)
                                onLogout()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                    )
                    ) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout")
                    }
                }
            }
        }
    }
}

/**
 * Creates a new content URI for storing a JPEG image in the external media store.
 *
 * The generated URI can be used as a destination for capturing a photo with the camera.
 *
 * @param context The context used to access the content resolver.
 * @return A URI for the new image, or null if the insertion fails.
 */
fun createImageUri(context: android.content.Context): Uri? {
    val contentResolver = context.contentResolver
    val contentValues =
            ContentValues().apply {
                put(
                        MediaStore.Images.Media.DISPLAY_NAME,
                        "profile_${System.currentTimeMillis()}.jpg"
                )
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
    return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
}

/**
 * Renders a design-time preview of the ProfileScreen composable using mock data and app theming.
 *
 * Intended for use in IDE previews to visualize the profile UI layout and appearance.
 */
@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    val context = LocalContext.current
    SafeChatTheme {
        Surface {
            ProfileScreen(
                    onNavigateBack = {},
                    onLogout = {},
                    viewModel = PreviewProfileViewModel(context)
            )
        }
    }
}
