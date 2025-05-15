package tech.ziasvannes.safechat.presentation.screens.profile

import android.Manifest
import android.content.ContentValues
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
 */
@OptIn(ExperimentalMaterial3Api::class)
/**
 * Displays a composable screen for viewing and editing the user's profile, including avatar, username, user ID, and cryptographic keys.
 *
 * Supports toggling between view and edit modes, updating the avatar via camera or gallery, copying the user ID and public key to the clipboard, and generating a new key pair with a warning about invalidating existing encrypted conversations. Handles loading and error states, and provides navigation back via the supplied callback.
 *
 * @param onNavigateBack Invoked when the user requests to navigate back from the profile screen.
 */
@Composable
fun ProfileScreen(onNavigateBack: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val clipboardManager = LocalClipboardManager.current
        val snackbarHostState = remember { SnackbarHostState() }

        // --- Image picker state and launchers ---
        var showImagePickerDialog by remember { mutableStateOf(false) }
        var tempCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

        // Camera launcher
        val cameraLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success
                        ->
                        if (success && tempCameraUri != null) {
                                viewModel.onEvent(
                                        ProfileEvent.OnAvatarSelected(context, tempCameraUri!!)
                                )
                        }
                }
        // Gallery launcher
        val galleryLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                        uri?.let { viewModel.onEvent(ProfileEvent.OnAvatarSelected(context, it)) }
                }

        // Permission launcher (for camera)
        val cameraPermissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                        granted ->
                        if (granted) {
                                val uri = createImageUri(context)
                                if (uri != null) {
                                        tempCameraUri = uri
                                        cameraLauncher.launch(uri)
                                }
                        } else {
                                Toast.makeText(
                                                context,
                                                "Camera permission denied",
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
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
                        if (result == SnackbarResult.ActionPerformed ||
                                        result == SnackbarResult.Dismissed
                        ) {
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
                                                IconButton(
                                                        onClick = {
                                                                viewModel.onEvent(
                                                                        ProfileEvent.SaveProfile
                                                                )
                                                        }
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.Default
                                                                                .Clear, // TODO Save
                                                                contentDescription = "Save profile"
                                                        )
                                                }
                                        } else {
                                                IconButton(
                                                        onClick = {
                                                                viewModel.onEvent(
                                                                        ProfileEvent.ToggleEditMode
                                                                )
                                                        }
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
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .border(
                                                        2.dp,
                                                        MaterialTheme.colorScheme.primary,
                                                        CircleShape
                                                )
                                                .clickable(enabled = state.isEditMode) {
                                                        showImagePickerDialog = true
                                                },
                                contentAlignment = Alignment.Center
                        ) {
                                if (!state.avatar.isNullOrBlank()) {
                                        val imageBitmap =
                                                remember(state.avatar) {
                                                        try {
                                                                val decodedBytes =
                                                                        Base64.decode(
                                                                                state.avatar,
                                                                                Base64.DEFAULT
                                                                        )
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
                                                        modifier =
                                                                Modifier.size(120.dp)
                                                                        .clip(CircleShape)
                                                )
                                        } else {
                                                Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = "Profile avatar",
                                                        tint =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
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
                                                                .background(
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                                )
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
                                        text = {
                                                Text("Choose a method to set your profile picture.")
                                        },
                                        confirmButton = {
                                                Column {
                                                        Button(
                                                                onClick = {
                                                                        showImagePickerDialog =
                                                                                false
                                                                        val cameraPermission =
                                                                                Manifest.permission
                                                                                        .CAMERA
                                                                        if (ContextCompat
                                                                                        .checkSelfPermission(
                                                                                                context,
                                                                                                cameraPermission
                                                                                        ) ==
                                                                                        PackageManager
                                                                                                .PERMISSION_GRANTED
                                                                        ) {
                                                                                val uri =
                                                                                        createImageUri(
                                                                                                context
                                                                                        )
                                                                                if (uri != null) {
                                                                                        tempCameraUri =
                                                                                                uri
                                                                                        cameraLauncher
                                                                                                .launch(
                                                                                                        uri
                                                                                                )
                                                                                }
                                                                        } else {
                                                                                cameraPermissionLauncher
                                                                                        .launch(
                                                                                                cameraPermission
                                                                                        )
                                                                        }
                                                                }
                                                        ) {
                                                                Icon(
                                                                        Icons.Default
                                                                                .Favorite, // TODO
                                                                        // PhotoCamera
                                                                        contentDescription = null
                                                                )
                                                                Spacer(Modifier.width(8.dp))
                                                                Text("Take Photo")
                                                        }
                                                        Spacer(Modifier.height(8.dp))
                                                        Button(
                                                                onClick = {
                                                                        showImagePickerDialog =
                                                                                false
                                                                        galleryLauncher.launch(
                                                                                "image/*"
                                                                        )
                                                                }
                                                        ) {
                                                                Icon(
                                                                        Icons.Default
                                                                                .Favorite, // TODO
                                                                        // PhotoLibrary
                                                                        contentDescription = null
                                                                )
                                                                Spacer(Modifier.width(8.dp))
                                                                Text("Choose from Gallery")
                                                        }
                                                }
                                        },
                                        dismissButton = {
                                                TextButton(
                                                        onClick = { showImagePickerDialog = false }
                                                ) { Text("Cancel") }
                                        }
                                )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Username
                        if (state.isEditMode) {
                                CustomTextField(
                                        value = state.userName,
                                        onValueChange = {
                                                viewModel.onEvent(
                                                        ProfileEvent.OnUserNameChanged(it)
                                                )
                                        },
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
                                                val formattedUuid = formatUuid(state.userId)
                                                Text(
                                                        text = "ID: $formattedUuid",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                                IconButton(
                                                        onClick = {
                                                                clipboardManager.setText(
                                                                        AnnotatedString(
                                                                                state.userId
                                                                        )
                                                                )
                                                                Toast.makeText(
                                                                                context,
                                                                                "User ID copied to clipboard",
                                                                                Toast.LENGTH_SHORT
                                                                        )
                                                                        .show()
                                                        }
                                                ) {
                                                        Icon(
                                                                Icons.Default.Favorite,
                                                                contentDescription = "Copy ID"
                                                        )
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
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Text(
                                                                text = "Your Public Key",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleMedium
                                                        )

                                                        IconButton(
                                                                onClick = {
                                                                        viewModel.onEvent(
                                                                                ProfileEvent
                                                                                        .ToggleKeyVisibility
                                                                        )
                                                                }
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                if (state.isKeyVisible
                                                                                )
                                                                                        Icons.Default
                                                                                                .FavoriteBorder
                                                                                else
                                                                                        Icons.Default
                                                                                                .Favorite, // TODO VisibilityOff,
                                                                        // Visibility
                                                                        contentDescription =
                                                                                if (state.isKeyVisible
                                                                                )
                                                                                        "Hide key"
                                                                                else "Show key"
                                                                )
                                                        }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Public key display
                                                if (state.isKeyVisible) {
                                                        val displayKey =
                                                                state.userPublicKey
                                                                        .replace("/", "")
                                                                        .chunked(4)
                                                                        .joinToString("-")
                                                        Surface(
                                                                modifier =
                                                                        Modifier.fillMaxWidth()
                                                                                .padding(
                                                                                        vertical =
                                                                                                8.dp
                                                                                ),
                                                                shape = MaterialTheme.shapes.medium,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceVariant
                                                        ) {
                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                                        .padding(
                                                                                                16.dp
                                                                                        ),
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        Text(
                                                                                text = displayKey,
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyMedium,
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                1f
                                                                                        )
                                                                        )
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.width(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        IconButton(
                                                                                onClick = {
                                                                                        clipboardManager
                                                                                                .setText(
                                                                                                        AnnotatedString(
                                                                                                                state.userPublicKey
                                                                                                        )
                                                                                                )
                                                                                        Toast.makeText(
                                                                                                        context,
                                                                                                        "Public key copied to clipboard",
                                                                                                        Toast.LENGTH_SHORT
                                                                                                )
                                                                                                .show()
                                                                                        viewModel
                                                                                                .onEvent(
                                                                                                        ProfileEvent
                                                                                                                .CopyPublicKey
                                                                                                )
                                                                                }
                                                                        ) {
                                                                                Icon(
                                                                                        imageVector =
                                                                                                Icons.Default
                                                                                                        .FavoriteBorder, // TODO ContentCopy
                                                                                        contentDescription =
                                                                                                "Copy public key"
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                } else {
                                                        Text(
                                                                text = "••••••••••••••••••••••",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium
                                                        )
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                // Generate new key pair button
                                                Button(
                                                        onClick = {
                                                                // Show confirmation dialog before
                                                                // generating new keys
                                                                // For now, just generate directly
                                                                viewModel.onEvent(
                                                                        ProfileEvent
                                                                                .GenerateNewKeyPair
                                                                )
                                                                Toast.makeText(
                                                                                context,
                                                                                "New key pair generated",
                                                                                Toast.LENGTH_SHORT
                                                                        )
                                                                        .show()
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
                                                        text =
                                                                "Warning: Generating a new key pair will invalidate all existing encrypted conversations.",
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
 * Formats a string as a canonical UUID (8-4-4-4-12 dashed pattern) if it contains exactly 32
 * hexadecimal characters.
 *
 * If the input string, after removing dashes, has 32 characters, it is reformatted as a standard
 * UUID. Otherwise, the original string is returned.
 *
 * @param uuid The string to attempt to format as a UUID.
 * @return The formatted UUID string, or the original input if formatting is not possible.
 */
fun formatUuid(uuid: String): String {
        val clean = uuid.replace("-", "")
        return if (clean.length == 32) {
                "${clean.substring(0,8)}-${clean.substring(8,12)}-${clean.substring(12,16)}-${clean.substring(16,20)}-${clean.substring(20)}"
        } else uuid
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
                                viewModel = PreviewProfileViewModel(context)
                        )
                }
        }
}
