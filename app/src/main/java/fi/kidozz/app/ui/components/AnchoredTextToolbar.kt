package fi.kidozz.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * A custom text toolbar that appears anchored directly above text fields
 * instead of floating around the screen.
 */
@Composable
fun AnchoredTextToolbar(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    var showToolbar by remember { mutableStateOf(false) }
    var toolbarRect by remember { mutableStateOf(Rect.Zero) }
    val clipboardManager = LocalClipboardManager.current

    // Calculate if we have selected text
    val hasSelection = textFieldValue.selection != TextRange.Zero
    val selectedText = if (hasSelection) {
        textFieldValue.text.substring(
            textFieldValue.selection.min,
            textFieldValue.selection.max
        )
    } else ""

    // Show toolbar when there's a selection or when long-pressing
    LaunchedEffect(textFieldValue.selection) {
        if (textFieldValue.selection != TextRange.Zero) {
            showToolbar = true
        }
    }

    // Toolbar actions
    val actions = listOf(
        TextAction(
            label = "Cut",
            icon = Icons.Default.ContentCut,
            enabled = hasSelection,
            onClick = {
                if (hasSelection) {
                    clipboardManager.setText(AnnotatedString(selectedText))
                    onValueChange(
                        textFieldValue.copy(
                            text = textFieldValue.text.removeRange(
                                textFieldValue.selection.min,
                                textFieldValue.selection.max
                            ),
                            selection = TextRange(textFieldValue.selection.min)
                        )
                    )
                }
                showToolbar = false
            }
        ),
        TextAction(
            label = "Copy",
            icon = Icons.Default.ContentCopy,
            enabled = hasSelection,
            onClick = {
                if (hasSelection) {
                    clipboardManager.setText(AnnotatedString(selectedText))
                }
                showToolbar = false
            }
        ),
        TextAction(
            label = "Paste",
            icon = Icons.Default.ContentPaste,
            enabled = clipboardManager.getText()?.text?.isNotEmpty() == true,
            onClick = {
                val clipboardText = clipboardManager.getText()?.text ?: ""
                val newText = textFieldValue.text.replaceRange(
                    textFieldValue.selection.min,
                    textFieldValue.selection.max,
                    clipboardText
                )
                onValueChange(
                    textFieldValue.copy(
                        text = newText,
                        selection = TextRange(
                            textFieldValue.selection.min + clipboardText.length
                        )
                    )
                )
                showToolbar = false
            }
        ),
        TextAction(
            label = "Select All",
            icon = Icons.Default.SelectAll,
            enabled = textFieldValue.text.isNotEmpty(),
            onClick = {
                onValueChange(
                    textFieldValue.copy(
                        selection = TextRange(0, textFieldValue.text.length)
                    )
                )
                showToolbar = true // Keep toolbar open after select all
            }
        )
    )

    // Show toolbar popup
    if (showToolbar) {
        Popup(
            alignment = Alignment.TopCenter,
            offset = androidx.compose.ui.unit.IntOffset(
                x = toolbarRect.center.x.toInt() - 100, // Center the toolbar
                y = (toolbarRect.top - 60).toInt() // Position above the text field
            ),
            onDismissRequest = { showToolbar = false },
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = Modifier
                    .width(200.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF424242) // Dark background
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(4.dp)
                ) {
                    actions.forEach { action ->
                        TextToolbarAction(
                            action = action,
                            onClick = action.onClick
                        )
                        if (action != actions.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }

    // Store toolbar rect for positioning
    LaunchedEffect(Unit) {
        // This would be set by the text field when it gets focus
        // For now, we'll use a default position
        toolbarRect = Rect(0f, 0f, 200f, 100f)
    }
}

/**
 * Represents a text action in the toolbar
 */
data class TextAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val enabled: Boolean,
    val onClick: () -> Unit
)

/**
 * Individual action item in the toolbar
 */
@Composable
private fun TextToolbarAction(
    action: TextAction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = action.enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.label,
            tint = if (action.enabled) Color.White else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = action.label,
            color = if (action.enabled) Color.White else Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
