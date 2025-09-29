package fi.kidozz.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A reusable full-screen dialog component that can be used throughout the app.
 * 
 * @param isVisible Whether the dialog is currently visible
 * @param onDismiss Callback when the dialog should be dismissed
 * @param title Optional title for the dialog
 * @param content The main content of the dialog
 * @param actions Optional action buttons at the bottom
 * @param modifier Modifier for the dialog
 */
@Composable
fun FullScreenDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable ColumnScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = true
            )
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(3.dp)
                    .windowInsetsPadding(WindowInsets.ime),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Optional title
                    title?.let { titleText ->
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = MaterialTheme.typography.headlineSmall.fontWeight,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Main content - scrollable area
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        content()
                    }
                    
                    // Action buttons - always visible at bottom
                    Spacer(modifier = Modifier.height(16.dp))
                    Column {
                        actions()
                    }
                }
            }
        }
    }
}
