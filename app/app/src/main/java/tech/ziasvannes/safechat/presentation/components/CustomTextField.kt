package tech.ziasvannes.safechat.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.ziasvannes.safechat.presentation.theme.SafeChatTheme

/**
 * A styled text field component with customizable appearance and behavior.
 *
 * @param value The current text value
 * @param onValueChange Callback invoked when the text value changes
 * @param modifier Optional Modifier for styling
 * @param label Optional label to display
 * @param placeholder Optional placeholder text
 * @param leadingIcon Optional leading icon
 * @param trailingIcon Optional trailing icon
 * @param isError Whether the field is in an error state
 * @param errorMessage Error message to display when isError is true
 * @param keyboardOptions Options controlling keyboard behavior
 * @param keyboardActions Actions to perform based on keyboard input
 * @param singleLine Whether the field should be single line
 * @param maxLines Maximum number of lines
 * @param shape The shape of the text field
 * @param colors Custom colors for the text field
 * @param visualTransformation Transformation to apply to the input (e.g., for password fields)
 * @param focusRequester Optional FocusRequester for programmatic focus management
 */
@OptIn(ExperimentalMaterial3Api::class)
/**
 * Displays a customizable text input field with optional label, placeholder, icons, error state, and keyboard behavior.
 *
 * Supports single-line or multi-line input, animated error message display, visual transformations, and programmatic focus control.
 *
 * @param value The current text to display in the field.
 * @param onValueChange Callback invoked when the text changes.
 * @param label Optional label text shown above the field.
 * @param placeholder Optional placeholder text shown when the field is empty.
 * @param leadingIcon Optional composable displayed at the start of the field.
 * @param trailingIcon Optional composable displayed at the end of the field.
 * @param isError If true, displays the field and error message in an error state.
 * @param errorMessage Optional error message shown below the field when `isError` is true and the message is not blank.
 * @param keyboardOptions Keyboard configuration for input type and IME actions.
 * @param keyboardActions Actions triggered by keyboard events.
 * @param singleLine If true, restricts input to a single line.
 * @param maxLines Maximum number of lines for input; defaults to unlimited unless `singleLine` is true.
 * @param shape Shape of the text field's outline.
 * @param colors Color scheme for the text field.
 * @param visualTransformation Optional visual transformation for the input text (e.g., password masking).
 * @param focusRequester Optional focus requester for programmatic focus control.
 */
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: TextFieldColors = TextFieldDefaults.colors(
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent
    ),
    visualTransformation: VisualTransformation = VisualTransformation.None,
    focusRequester: FocusRequester? = null
) {
    Box(modifier = modifier) {
        val actualModifier = if (focusRequester != null) {
            Modifier.focusRequester(focusRequester)
        } else {
            Modifier
        }

        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = actualModifier.fillMaxWidth(),
            label = label?.let { { Text(text = it) } },
            placeholder = placeholder?.let { { Text(text = it) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            shape = shape,
            colors = colors,
            visualTransformation = visualTransformation
        )

        // Show error message if needed
        AnimatedVisibility(visible = isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Displays a single-line search input field with a search icon and a clear button.
 *
 * The field shows a search icon on the left and, when text is entered, a clear button on the right. Pressing the search action on the keyboard triggers the provided search callback.
 *
 * @param value The current text in the search field.
 * @param onValueChange Called when the text input changes.
 * @param modifier Modifier for styling and layout.
 * @param placeholder Placeholder text shown when the field is empty.
 * @param onSearch Called when the search action is triggered from the keyboard.
 */
@Composable
fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    onSearch: (String) -> Unit = {}
) {
    CustomTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch(value) }
        )
    )
}

/**
 * Preview of the CustomTextField composable displaying a labeled text input with a placeholder.
 *
 * Shows how to use CustomTextField within a themed surface and padded box for UI demonstration purposes.
 */
@Preview(showBackground = true)
@Composable
fun CustomTextFieldPreview() {
    SafeChatTheme {
        var text by remember { mutableStateOf("") }
        var searchText by remember { mutableStateOf("") }
        
        Surface {
            Box(modifier = Modifier.padding(16.dp)) {
                CustomTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = "Message",
                    placeholder = "Type a message"
                )
            }
        }
    }
}

/**
 * Displays a preview of the `SearchTextField` composable with a placeholder for searching contacts.
 *
 * This preview shows the search field inside a padded box using the app's theme.
 */
@Preview(showBackground = true)
@Composable
fun SearchTextFieldPreview() {
    SafeChatTheme {
        var searchText by remember { mutableStateOf("") }
        
        Surface {
            Box(modifier = Modifier.padding(16.dp)) {
                SearchTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = "Search contacts"
                )
            }
        }
    }
}

/**
 * Preview of the CustomTextField composable in an error state with an error message.
 *
 * Displays a text field labeled "Email" showing the error message "Invalid email address" for UI demonstration purposes.
 */
@Preview(showBackground = true)
@Composable
fun CustomTextFieldErrorPreview() {
    SafeChatTheme {
        var text by remember { mutableStateOf("") }
        
        Surface {
            Box(modifier = Modifier.padding(16.dp)) {
                CustomTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = "Email",
                    isError = true,
                    errorMessage = "Invalid email address"
                )
            }
        }
    }
}