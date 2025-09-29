package fi.kidozz.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * A reusable dropdown component that includes the title inside the dropdown box.
 * The title appears as a placeholder when no option is selected.
 * 
 * @param title The title/placeholder text to show in the dropdown
 * @param options List of available options to choose from
 * @param selectedValue Currently selected value (empty string if none selected)
 * @param onValueChange Callback when a new value is selected
 * @param modifier Modifier for the dropdown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleDropdown(
    title: String,
    options: List<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = if (selectedValue.isEmpty()) title else selectedValue.replaceFirstChar { it.uppercase() },
            onValueChange = { },
            readOnly = true,
            enabled = true,
            placeholder = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFED9738), // Same yellow as text field
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                unfocusedTextColor = if (selectedValue.isEmpty()) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                unfocusedBorderColor = if (selectedValue.isEmpty()) {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) // Keep grey when not focused
                } else {
                    MaterialTheme.colorScheme.outline // Keep grey when not focused
                },
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFFFFF9F1)) // Light yellow background with 100% opacity
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = option.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
