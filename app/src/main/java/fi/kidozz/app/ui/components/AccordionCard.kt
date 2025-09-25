package fi.kidozz.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fi.kidozz.app.ui.theme.SecondaryTextColor

/**
 * A reusable accordion card component that can be used throughout the app.
 * 
 * @param title The main title text displayed in the collapsed state
 * @param subtitle Optional subtitle text displayed below the title
 * @param expandedContent The content to show when the accordion is expanded
 * @param onChatClick Optional callback for chat button click
 * @param showChatButton Whether to show the chat button (default: true)
 * @param modifier Modifier for the entire accordion card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccordionCard(
    title: String,
    subtitle: String? = null,
    expandedContent: @Composable () -> Unit,
    onChatClick: (() -> Unit)? = null,
    showChatButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Main card content
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE0E0E0))
                )

                // Content
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Collapsed state - title, subtitle, and action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (subtitle != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SecondaryTextColor
                                )
                            }
                        }
                        
                        // Chat icon (optional)
                        if (showChatButton && onChatClick != null) {
                            IconButton(
                                onClick = onChatClick
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "Chat",
                                    tint = SecondaryTextColor
                                )
                            }
                        }
                        
                        // Expand/collapse icon
                        IconButton(
                            onClick = { expanded = !expanded }
                        ) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                tint = SecondaryTextColor
                            )
                        }
                    }

                    // Expanded state - custom content
                    if (expanded) {
                        HorizontalDivider()
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            expandedContent()
                        }
                    }
                }

                // Bottom border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE0E0E0))
                )
            }
        }
    }
}

/**
 * A specialized accordion card for kids with status indicator and specific styling.
 * 
 * @param kidName The kid's name (will be displayed in uppercase)
 * @param status The kid's status (will be displayed in uppercase)
 * @param expandedContent The content to show when expanded
 * @param onChatClick Optional callback for chat button click
 * @param modifier Modifier for the entire accordion card
 */
@Composable
fun KidAccordionCard(
    kidName: String,
    status: String,
    expandedContent: @Composable () -> Unit,
    onChatClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AccordionCard(
        title = kidName.uppercase(),
        subtitle = "Status: ${status.uppercase()}",
        expandedContent = expandedContent,
        onChatClick = onChatClick,
        showChatButton = true,
        modifier = modifier
    )
}
