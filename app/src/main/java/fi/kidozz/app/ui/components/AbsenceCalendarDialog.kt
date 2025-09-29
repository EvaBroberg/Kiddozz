package fi.kidozz.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.kidozz.app.ui.styles.BasicButton
import fi.kidozz.app.ui.styles.WarningButton
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * A calendar dialog for selecting absence dates.
 * Allows parents to select multiple days for reporting their child's absence.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsenceCalendarDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onAbsenceSelected: (List<LocalDate>, String, String) -> Unit,
    kidName: String,
    absenceReasons: List<String> = listOf("sick", "holiday"), // Default fallback
    modifier: Modifier = Modifier
) {
    var selectedDates by remember { mutableStateOf(setOf(LocalDate.now())) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedReason by remember { mutableStateOf(absenceReasons.firstOrNull() ?: "sick") }
    var expanded by remember { mutableStateOf(false) }
    var absenceDetails by remember { mutableStateOf("") }
    
    FullScreenDialog(
        isVisible = isVisible,
        onDismiss = onDismiss,
        modifier = modifier,
        content = {
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
            
            // Selected dates summary
            if (selectedDates.isNotEmpty()) {
                Text(
                    text = "Selected ${selectedDates.size} day(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFED9738), // Same yellow as warning button
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
            
            // Reason for absence dropdown
            Text(
                text = "Reason for absence",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedReason.replaceFirstChar { it.uppercase() },
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    absenceReasons.forEach { reason ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = reason.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = {
                                selectedReason = reason
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Absence details text field
            Text(
                text = "Provide details of absence",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = absenceDetails,
                onValueChange = { absenceDetails = it },
                placeholder = { 
                    Text(
                        text = "Enter additional details about the absence...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedReason.isNotEmpty(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                ),
                maxLines = 3,
                textStyle = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        },
        actions = {
            BasicButton(
                text = "Cancel",
                onClick = onDismiss
            )
            
            WarningButton(
                text = "Submit Absence",
                onClick = {
                    onAbsenceSelected(selectedDates.sorted(), selectedReason, absenceDetails)
                    onDismiss()
                },
                enabled = selectedDates.isNotEmpty()
            )
        }
    )
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
                                isSelected -> Color(0xFFED9738) // Yellow color from warning button
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
                            isSelected -> Color.White
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