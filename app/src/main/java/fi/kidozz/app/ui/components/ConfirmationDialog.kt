package fi.kidozz.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * A confirmation dialog that shows a success message and auto-dismisses after a specified duration.
 * 
 * @param isVisible Whether the dialog is currently visible
 * @param message The confirmation message to display
 * @param onDismiss Callback when the dialog is dismissed
 * @param durationMs How long the dialog should be visible (default 5000ms)
 * @param modifier Modifier for the dialog
 */
@Composable
fun ConfirmationDialog(
    isVisible: Boolean,
    message: String,
    onDismiss: () -> Unit,
    durationMs: Long = 5000L,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                exit = scaleOut(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200)),
                modifier = modifier
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box {
                        // Close button in top-right corner
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF9E9E9E)
                            )
                        }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .padding(top = 8.dp), // Extra padding to account for close button
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Success icon (using a simple circle with checkmark)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        Color(0xFF4CAF50),
                                        shape = RoundedCornerShape(24.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✓",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Confirmation message
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Auto-dismiss countdown
                            Text(
                                text = "This dialog will close automatically",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Auto-dismiss after specified duration
    if (isVisible) {
        LaunchedEffect(isVisible) {
            delay(durationMs)
            onDismiss()
        }
    }
}

/**
 * Helper function to format the absence confirmation message
 */
fun formatAbsenceConfirmationMessage(
    childName: String,
    firstDayBack: LocalDate
): String {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val formattedDate = firstDayBack.format(formatter)
    return "$childName away until $formattedDate"
}
