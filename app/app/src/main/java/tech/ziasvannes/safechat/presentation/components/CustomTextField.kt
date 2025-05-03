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
 * A customizable text input field for Jetpack Compose with support for labels, placeholders, icons, error states, keyboard options, and visual transformations.
 *
 * Displays an optional error message below the field when in error state. Supports single-line or multi-line input, custom shapes, and theming.
 *
 * @param value The current text to display in the field.
 * @param onValueChange Callback invoked when the text changes.
 * @param label Optional label displayed above the field.
 * @param placeholder Optional placeholder text shown when the field is empty.
 * @param leadingIcon Optional composable displayed at the start of the field.
 * @param trailingIcon Optional composable displayed at the end of the field.
 * @param isError Whether the field is in an error state.
 * @param errorMessage Optional error message shown below the field when in error state.
 * @param keyboardOptions Keyboard configuration for the field.
 * @param keyboardActions Keyboard action callbacks.
 * @param singleLine Whether the field is single-line.
 * @param maxLines Maximum number of lines for input.
 * @param shape The shape of the text field.
 * @param colors Color configuration for the text field.
 * @param visualTransformation Visual transformation applied to the input text.
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
 * Displays a single-line search input field with a leading search icon and a trailing clear button.
 *
 * The clear button appears when the input is non-empty and clears the text when pressed. Triggers the provided callback when the search action is performed from the keyboard.
 *
 * @param value The current text entered in the search field.
 * @param onValueChange Called when the search text changes or is cleared.
 * @param placeholder Optional placeholder text shown when the field is empty.
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
 * Preview composable displaying a basic `CustomTextField` with a label and placeholder.
 *
 * Shows the `CustomTextField` component within the app theme and padded surface for design-time visualization.
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
 * Preview of the `SearchTextField` composable displaying a search input with placeholder text.
 *
 * Demonstrates the appearance and behavior of the search field within the app theme.
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
 * Displays a text field labeled "Email" showing an error message "Invalid email address" for design-time visualization.
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