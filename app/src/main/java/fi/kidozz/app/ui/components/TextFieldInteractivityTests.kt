package fi.kidozz.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.kidozz.app.ui.theme.KiddozzTheme

/**
 * Preview tests to verify text field components are focusable and allow typing.
 * These previews help ensure interactivity is working correctly.
 */

@Preview(showBackground = true, name = "AppTextFieldSingleLine - Interactive")
@Composable
fun AppTextFieldSingleLinePreview() {
    KiddozzTheme {
        var text by remember { mutableStateOf("") }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("AppTextFieldSingleLine Test", style = MaterialTheme.typography.headlineSmall)
            
            AppTextFieldSingleLine(
                placeholder = "Enter your name",
                value = text,
                onValueChange = { text = it },
                enabled = true,
                keyboardType = KeyboardType.Text
            )
            
            Text("Current value: '$text'", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true, name = "AppTextFieldSingleLine - Disabled")
@Composable
fun AppTextFieldSingleLineDisabledPreview() {
    KiddozzTheme {
        var text by remember { mutableStateOf("Disabled field") }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("AppTextFieldSingleLine Disabled Test", style = MaterialTheme.typography.headlineSmall)
            
            AppTextFieldSingleLine(
                placeholder = "This should be disabled",
                value = text,
                onValueChange = { text = it },
                enabled = false,
                keyboardType = KeyboardType.Text
            )
            
            Text("Current value: '$text'", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true, name = "AppTextArea - Interactive")
@Composable
fun AppTextAreaPreview() {
    KiddozzTheme {
        var text by remember { mutableStateOf("") }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("AppTextArea Test", style = MaterialTheme.typography.headlineSmall)
            
            AppTextArea(
                placeholder = "Enter your notes here...",
                value = text,
                onValueChange = { text = it },
                enabled = true,
                maxLines = 5
            )
            
            Text("Current value: '$text'", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true, name = "AppTextArea - Disabled")
@Composable
fun AppTextAreaDisabledPreview() {
    KiddozzTheme {
        var text by remember { mutableStateOf("This textarea is disabled") }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("AppTextArea Disabled Test", style = MaterialTheme.typography.headlineSmall)
            
            AppTextArea(
                placeholder = "This should be disabled",
                value = text,
                onValueChange = { text = it },
                enabled = false,
                maxLines = 5
            )
            
            Text("Current value: '$text'", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true, name = "TitleTextField - Interactive")
@Composable
fun TitleTextFieldPreview() {
    KiddozzTheme {
        var text by remember { mutableStateOf("") }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("TitleTextField Test", style = MaterialTheme.typography.headlineSmall)
            
            TitleTextField(
                placeholder = "Enter your message",
                value = text,
                onValueChange = { text = it },
                enabled = true,
                maxLines = 15
            )
            
            Text("Current value: '$text'", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true, name = "TitleTextField - Disabled")
@Composable
fun TitleTextFieldDisabledPreview() {
    KiddozzTheme {
        var text by remember { mutableStateOf("This field is disabled") }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("TitleTextField Disabled Test", style = MaterialTheme.typography.headlineSmall)
            
            TitleTextField(
                placeholder = "This should be disabled",
                value = text,
                onValueChange = { text = it },
                enabled = false,
                maxLines = 15
            )
            
            Text("Current value: '$text'", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true, name = "All Text Fields - Comparison")
@Composable
fun AllTextFieldsComparisonPreview() {
    KiddozzTheme {
        var singleLineText by remember { mutableStateOf("") }
        var textAreaText by remember { mutableStateOf("") }
        var titleText by remember { mutableStateOf("") }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("All Text Field Components", style = MaterialTheme.typography.headlineSmall)
            
            Text("1. AppTextFieldSingleLine:", style = MaterialTheme.typography.titleMedium)
            AppTextFieldSingleLine(
                placeholder = "Single line input",
                value = singleLineText,
                onValueChange = { singleLineText = it },
                enabled = true
            )
            
            Text("2. AppTextArea:", style = MaterialTheme.typography.titleMedium)
            AppTextArea(
                placeholder = "Multi-line text area",
                value = textAreaText,
                onValueChange = { textAreaText = it },
                enabled = true
            )
            
            Text("3. TitleTextField:", style = MaterialTheme.typography.titleMedium)
            TitleTextField(
                placeholder = "Title text field",
                value = titleText,
                onValueChange = { titleText = it },
                enabled = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Values:", style = MaterialTheme.typography.titleMedium)
            Text("Single line: '$singleLineText'", style = MaterialTheme.typography.bodySmall)
            Text("Text area: '$textAreaText'", style = MaterialTheme.typography.bodySmall)
            Text("Title field: '$titleText'", style = MaterialTheme.typography.bodySmall)
        }
    }
}
