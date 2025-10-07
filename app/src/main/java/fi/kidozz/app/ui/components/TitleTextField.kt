package fi.kidozz.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * A reusable text field component with title as placeholder and common editing options.
 * 
 * @param placeholder The placeholder text to show in the text field
 * @param value Current text value
 * @param onValueChange Callback when text changes
 * @param enabled Whether the text field is enabled
 * @param modifier Modifier for the text field
 * @param maxLines Maximum number of lines (default 15)
 * @param keyboardType Type of keyboard to show (default Text)
 * @param capitalization Text capitalization (default Sentences)
 * @param imeAction Action for the IME (default Done)
 * @param onImeAction Callback for IME action
 */
@Composable
fun TitleTextField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxLines: Int = 15,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {}
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
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = imeAction,
            autoCorrectEnabled = true
        ),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = false
    )
}
