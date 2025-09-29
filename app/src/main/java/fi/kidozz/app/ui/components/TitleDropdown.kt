package fi.kidozz.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

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
                    style = MaterialTheme.typography.bodyMedium,
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
                unfocusedTextColor = if (selectedValue.isEmpty()) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                unfocusedBorderColor = if (selectedValue.isEmpty()) {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outline
                },
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = option.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium
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
