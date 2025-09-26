package fi.kidozz.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import fi.kidozz.app.ui.styles.BasicButton
import fi.kidozz.app.ui.styles.WarningButton
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * A calendar dialog for selecting absence dates.
 * Allows parents to select multiple days for reporting their child's absence.
 */
@Composable
fun AbsenceCalendarDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onAbsenceSelected: (List<LocalDate>) -> Unit,
    kidName: String,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        var selectedDates by remember { mutableStateOf(setOf<LocalDate>()) }
        var currentMonth by remember { mutableStateOf(YearMonth.now()) }
        
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    
                    // Month navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                currentMonth = currentMonth.minusMonths(1)
                            }
                        ) {
                            Text("‹", style = MaterialTheme.typography.headlineMedium)
                        }
                        
                        Text(
                            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        IconButton(
                            onClick = { 
                                currentMonth = currentMonth.plusMonths(1)
                            }
                        ) {
                            Text("›", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Calendar grid
                    AbsenceCalendarGrid(
                        yearMonth = currentMonth,
                        selectedDates = selectedDates,
                        onDateSelected = { date ->
                            selectedDates = if (date in selectedDates) {
                                selectedDates - date
                            } else {
                                selectedDates + date
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Selected dates summary
                    if (selectedDates.isNotEmpty()) {
                        Text(
                            text = "Selected ${selectedDates.size} day(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Action buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BasicButton(
                            text = "Cancel",
                            onClick = onDismiss
                        )
                        
                        WarningButton(
                            text = "Submit Absence",
                            onClick = {
                                onAbsenceSelected(selectedDates.sorted())
                                onDismiss()
                            },
                            enabled = selectedDates.isNotEmpty()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AbsenceCalendarGrid(
    yearMonth: YearMonth,
    selectedDates: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Convert to 0-based (Sunday = 0)
    val daysInMonth = yearMonth.lengthOfMonth()
    
    // Generate all dates for the month
    val dates = (1..daysInMonth).map { day ->
        yearMonth.atDay(day)
    }
    
    // Day headers
    val dayHeaders = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    
    Column(modifier = modifier) {
        // Day headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dayHeaders.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(280.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Empty cells for days before the first day of the month
            items(firstDayOfWeek) {
                Spacer(modifier = Modifier.size(32.dp))
            }
            
            // Days of the month
            items(dates) { date ->
                val isSelected = date in selectedDates
                val isToday = date == LocalDate.now()
                val isPast = date.isBefore(LocalDate.now())
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                else -> Color.Transparent
                            }
                        )
                        .clickable(enabled = !isPast) {
                            if (!isPast) {
                                onDateSelected(date)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
