package fi.kidozz.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * A reusable single-line text field component for simple inputs like names, emails, etc.
 * 
 * WHY WE SPLIT INTO TWO COMPONENTS:
 * - Single-line fields should not force height or multi-line behavior
 * - This component wraps content height naturally and focuses on single-line input
 * - Provides better UX for simple form fields that don't need textarea behavior
 * 
 * @param placeholder The placeholder text to show in the text field
 * @param value Current text value (state-driven - caller must provide)
 * @param onValueChange Callback when text changes (state-driven - caller must provide)
 * @param enabled Whether the text field is enabled
 * @param keyboardType Type of keyboard to show (default Text)
 * @param imeAction Action for the IME (default Done)
 * @param onImeAction Callback for IME action
 * @param modifier Modifier for the text field
 */
@Composable
fun AppTextFieldSingleLine(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = false, // Explicitly allow editing
        singleLine = true, // Always single line for this component
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
            autoCorrectEnabled = true
        ),
        keyboardActions = KeyboardActions(
            onDone = { onImeAction() },
            onNext = { onImeAction() },
            onSearch = { onImeAction() },
            onSend = { onImeAction() }
        ),
        colors = if (enabled) {
            OutlinedTextFieldDefaults.colors()
        } else {
            OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        },
        textStyle = MaterialTheme.typography.bodyMedium
    )
}
