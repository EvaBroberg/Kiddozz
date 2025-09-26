package fi.kidozz.app.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.ui.components.KidAccordionCard
import fi.kidozz.app.ui.components.AbsenceCalendarDialog
import fi.kidozz.app.ui.styles.WarningButton
import fi.kidozz.app.ui.theme.KiddozzTheme
import fi.kidozz.app.ui.theme.InCareColor
import fi.kidozz.app.ui.theme.OutColor
import fi.kidozz.app.ui.theme.SickColor
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    parentId: String,
    parentsViewModel: ParentsViewModel,
    modifier: Modifier = Modifier
) {
    val kids by parentsViewModel.kids.collectAsState()
    val isLoading by parentsViewModel.isLoading.collectAsState()
    val error by parentsViewModel.error.collectAsState()
    
    // Calendar dialog state
    var showAbsenceCalendar by remember { mutableStateOf(false) }
    var selectedKid by remember { mutableStateOf<Kid?>(null) }
    
    // Load kids when screen is first displayed
    LaunchedEffect(parentId) {
        parentsViewModel.loadKidsForParent(parentId)
    }
    
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(top = 24.dp)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Error loading kids",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { 
                                    parentsViewModel.clearError()
                                    parentsViewModel.loadKidsForParent(parentId)
                                }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                
                kids.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No kids found",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(0.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(kids) { kid ->
                            Column {
                                // Determine status color
                                val statusColor = when (kid.attendance.lowercase()) {
                                    "in-care" -> InCareColor
                                    "out" -> OutColor
                                    "sick" -> SickColor
                                    else -> OutColor // Default to out color
                                }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                ) {
                                    // Status indicator line
                                    Box(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .fillMaxHeight()
                                            .background(statusColor)
                                    )
                                    
                                    // Accordion card
                                    KidAccordionCard(
                                        kidName = kid.full_name,
                                        status = kid.attendance,
                                        onChatClick = { /* TODO: Implement chat functionality */ },
                                        expandedContent = {
                                            WarningButton(
                                                text = "Report Absence",
                                                onClick = { 
                                                    selectedKid = kid
                                                    showAbsenceCalendar = true
                                                }
                                            )
                                        }
                                    )
                                }
                                if (kid != kids.last()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Absence Calendar Dialog
        selectedKid?.let { kid ->
            AbsenceCalendarDialog(
                isVisible = showAbsenceCalendar,
                onDismiss = { 
                    showAbsenceCalendar = false
                    selectedKid = null
                },
                onAbsenceSelected = { selectedDates ->
                    // TODO: Implement absence reporting API call
                    // For now, just show a simple confirmation
                    // In a real app, this would call the backend API
                },
                kidName = kid.full_name
            )
        }
    }
}



@Preview(showBackground = true, name = "Parent Dashboard Screen Preview")
@Composable
fun ParentDashboardScreenPreview() {
    KiddozzTheme {
        // Note: This preview won't work without a real ViewModel
        // In a real app, you'd use a preview ViewModel or mock data
        Text("Parent Dashboard Preview")
    }
}
