package fi.kidozz.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * A reusable multi-line text area component for longer input like notes, descriptions, etc.
 * 
 * WHY WE SPLIT INTO TWO COMPONENTS:
 * - Multi-line textarea needs explicit height, multi-line support, and padding for readability
 * - This component provides a proper textarea experience with minimum height
 * - Separates concerns: single-line fields vs multi-line text areas have different UX needs
 * 
 * @param placeholder The placeholder text to show in the text area
 * @param value Current text value (state-driven - caller must provide)
 * @param onValueChange Callback when text changes (state-driven - caller must provide)
 * @param enabled Whether the text area is enabled
 * @param maxLines Maximum number of lines (default 5)
 * @param modifier Modifier for the text area
 */
@Composable
fun AppTextArea(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    maxLines: Int = 5,
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
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp), // Explicit minimum height so it looks like a textarea even when empty
        enabled = enabled,
        readOnly = false, // Explicitly allow editing
        singleLine = false, // Always multi-line for this component
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            autoCorrectEnabled = true
        ),
        colors = if (enabled) {
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFED9738), // Same yellow as selected days
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = Color(0xFFED9738), // Same yellow as selected days
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        },
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2 // Better line spacing for readability
        )
    )
}
