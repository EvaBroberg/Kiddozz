package fi.kidozz.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.kidozz.app.ui.theme.KiddozzTheme
import java.time.LocalDate

/**
 * Debug preview for AbsenceCalendarDialog to test text field interactivity.
 * This helps us verify that the text field becomes enabled when a reason is selected.
 */
@Preview(showBackground = true, name = "AbsenceCalendarDialog - Debug")
@Composable
fun AbsenceCalendarDialogDebugPreview() {
    KiddozzTheme {
        var selectedReason by remember { mutableStateOf("") }
        var absenceDetails by remember { mutableStateOf("") }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Debug: AbsenceCalendarDialog Text Field", style = MaterialTheme.typography.headlineSmall)
            
            // Show current state
            Text("Selected Reason: '$selectedReason'", style = MaterialTheme.typography.bodyMedium)
            Text("Absence Details: '$absenceDetails'", style = MaterialTheme.typography.bodyMedium)
            Text("Text Field Enabled: ${selectedReason.isNotEmpty()}", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Test the dropdown
            TitleDropdown(
                title = "Reason for absence",
                options = listOf("sick", "holiday"),
                selectedValue = selectedReason,
                onValueChange = { newValue -> 
                    selectedReason = newValue
                    println("DEBUG: Dropdown selected: $newValue")
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Test the text field
            AppTextArea(
                placeholder = "Provide details of absence",
                value = absenceDetails,
                onValueChange = { newText -> 
                    absenceDetails = newText
                    println("DEBUG: Text field changed: $newText")
                },
                enabled = selectedReason.isNotEmpty(),
                maxLines = 5
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Manual test buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { selectedReason = "sick" }
                ) {
                    Text("Set Sick")
                }
                
                Button(
                    onClick = { selectedReason = "holiday" }
                ) {
                    Text("Set Holiday")
                }
                
                Button(
                    onClick = { selectedReason = "" }
                ) {
                    Text("Clear Reason")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Dropdown Test")
@Composable
fun DropdownTestPreview() {
    KiddozzTheme {
        var selectedValue by remember { mutableStateOf("") }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Dropdown Test", style = MaterialTheme.typography.headlineSmall)
            
            Text("Selected: '$selectedValue'", style = MaterialTheme.typography.bodyMedium)
            
            TitleDropdown(
                title = "Select an option",
                options = listOf("option1", "option2", "option3"),
                selectedValue = selectedValue,
                onValueChange = { newValue -> 
                    selectedValue = newValue
                    println("DEBUG: Dropdown test selected: $newValue")
                }
            )
            
            // Test buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { selectedValue = "option1" }
                ) {
                    Text("Set Option 1")
                }
                
                Button(
                    onClick = { selectedValue = "option2" }
                ) {
                    Text("Set Option 2")
                }
                
                Button(
                    onClick = { selectedValue = "" }
                ) {
                    Text("Clear")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "AppTextArea - Direct Test")
@Composable
fun AppTextAreaDirectTestPreview() {
    KiddozzTheme {
        var text by remember { mutableStateOf("") }
        var enabled by remember { mutableStateOf(true) }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Direct AppTextArea Test", style = MaterialTheme.typography.headlineSmall)
            
            Text("Current text: '$text'", style = MaterialTheme.typography.bodyMedium)
            Text("Enabled: $enabled", style = MaterialTheme.typography.bodyMedium)
            
            // Test enabled/disabled toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { enabled = !enabled }
                ) {
                    Text(if (enabled) "Disable" else "Enable")
                }
                
                Button(
                    onClick = { text = "" }
                ) {
                    Text("Clear")
                }
            }
            
            // Test the text area
            AppTextArea(
                placeholder = "Type something here...",
                value = text,
                onValueChange = { newText -> 
                    text = newText
                    println("DEBUG: AppTextArea changed: $newText")
                },
                enabled = enabled,
                maxLines = 5
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Show the minimal AppTextArea
            Text("Minimal AppTextArea:", style = MaterialTheme.typography.titleMedium)
            
            AppTextAreaMinimal(
                placeholder = "Minimal text area",
                value = text,
                onValueChange = { text = it },
                enabled = enabled
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Show the simplified AppTextArea
            Text("Simplified AppTextArea:", style = MaterialTheme.typography.titleMedium)
            
            AppTextAreaSimple(
                placeholder = "Simplified text area",
                value = text,
                onValueChange = { text = it },
                enabled = enabled,
                maxLines = 5
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Show the raw OutlinedTextField for comparison
            Text("Raw OutlinedTextField for comparison:", style = MaterialTheme.typography.titleMedium)
            
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Raw text field") },
                enabled = enabled,
                readOnly = false,
                singleLine = false,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
