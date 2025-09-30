package fi.kidozz.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.kidozz.app.ui.components.AppTextArea
import fi.kidozz.app.ui.components.ConfirmationDialog
import fi.kidozz.app.ui.components.WarningSnackbar
import fi.kidozz.app.ui.styles.BasicButton
import fi.kidozz.app.ui.styles.WarningButton
import fi.kidozz.app.data.repository.KidsRepository
import kotlinx.coroutines.launch
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
    kidId: String,
    kidsRepository: KidsRepository,
    absenceReasons: List<String> = listOf("sick", "holiday"), // Default fallback
    modifier: Modifier = Modifier
) {
    var selectedDates by remember { mutableStateOf(setOf(LocalDate.now())) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedReason by remember { mutableStateOf("") }
    var absenceDetails by remember { mutableStateOf("") }
    var showWarning by remember { mutableStateOf(false) }
    var existingAbsences by remember { mutableStateOf<Map<LocalDate, String>>(emptyMap()) }
    var showConfirmation by remember { mutableStateOf(false) }
    var confirmationMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
    // Fetch existing absences when dialog opens
    LaunchedEffect(isVisible, kidId) {
        if (isVisible) {
            try {
                val result = kidsRepository.getAbsences(kidId)
                if (result.isSuccess) {
                    val absences = result.getOrNull() ?: emptyList()
                    existingAbsences = absences.associate { absence ->
                        LocalDate.parse(absence["date"] as String) to (absence["reason"] as String)
                    }
                } else {
                    existingAbsences = emptyMap()
                }
            } catch (e: Exception) {
                // Handle error silently for now
                existingAbsences = emptyMap()
            }
        }
    }
    
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
                existingAbsences = existingAbsences,
                onDateSelected = { date ->
                    // Check if date already has an absence
                    if (existingAbsences.containsKey(date)) {
                        showWarning = true
                        return@AbsenceCalendarGrid
                    }
                    
                    selectedDates = if (date in selectedDates) {
                        selectedDates - date
                    } else {
                        selectedDates + date
                    }
                }
            )
            
            // Selected dates summary
            if (selectedDates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Selected ${selectedDates.size} day(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFED9738), // Same yellow as warning button
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
            
            Spacer(modifier = Modifier.height(50.dp))
            
            // Reason for absence dropdown
            TitleDropdown(
                title = "Reason for absence",
                options = absenceReasons,
                selectedValue = selectedReason,
                onValueChange = { selectedReason = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Absence details text field with warning
            Box {
                AppTextArea(
                    placeholder = "Provide details of absence",
                    value = absenceDetails,
                    onValueChange = { absenceDetails = it },
                    enabled = selectedReason.isNotEmpty(),
                    maxLines = 5,
                    modifier = Modifier.clickable(
                        enabled = selectedReason.isEmpty()
                    ) {
                        if (selectedReason.isEmpty()) {
                            showWarning = true
                        }
                    }
                )
                
                // Warning positioned absolutely under the text field
                WarningSnackbar(
                    isVisible = showWarning,
                    message = if (selectedReason.isEmpty()) {
                        "Please select reason of absence"
                    } else {
                        "Some selected days already have absences recorded"
                    },
                    onDismiss = { showWarning = false },
                    durationMs = 3000L,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 120.dp) // Position under the text field
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        },
        actions = {
            // Use your custom button components
            BasicButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            WarningButton(
                text = "Submit Absence",
                onClick = {
                    if (selectedDates.isNotEmpty()) {
                        // Check if any selected dates overlap with existing absences
                        val overlappingDates = selectedDates.filter { existingAbsences.containsKey(it) }
                        if (overlappingDates.isNotEmpty()) {
                            showWarning = true
                            return@WarningButton
                        }
                        
                        // Calculate first day back (day after the last selected day)
                        val sortedDates = selectedDates.sorted()
                        val lastAbsenceDay = sortedDates.last()
                        val firstDayBack = lastAbsenceDay.plusDays(1)
                        
                        // Create confirmation message
                        confirmationMessage = formatAbsenceConfirmationMessage(kidName, firstDayBack)
                        
                        // Submit the absence
                        onAbsenceSelected(sortedDates, selectedReason, absenceDetails)
                        
                        // Close the absence dialog
                        onDismiss()
                        
                        // Show confirmation dialog
                        showConfirmation = true
                    } else {
                        // Show warning for no days selected
                        showWarning = true
                    }
                },
                enabled = selectedDates.isNotEmpty() && selectedReason.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
    
    // Confirmation dialog
    ConfirmationDialog(
        isVisible = showConfirmation,
        message = confirmationMessage,
        onDismiss = { showConfirmation = false },
        durationMs = 5000L
    )
}

@Composable
private fun AbsenceCalendarGrid(
    yearMonth: YearMonth,
    selectedDates: Set<LocalDate>,
    existingAbsences: Map<LocalDate, String>,
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
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Calendar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
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
                val hasExistingAbsence = existingAbsences.containsKey(date)
                val existingAbsenceReason = existingAbsences[date]
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                hasExistingAbsence -> when (existingAbsenceReason) {
                                    "holiday" -> Color(0xFFED9738) // Yellow for holiday
                                    "sick" -> Color.Red // Red for sick
                                    else -> Color.Gray // Fallback
                                }
                                isSelected -> Color(0xFFED9738) // Yellow color from warning button
                                isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                else -> Color.Transparent
                            }
                        )
                        .clickable(enabled = !isPast && !hasExistingAbsence) {
                            if (!isPast && !hasExistingAbsence) {
                                onDateSelected(date)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            hasExistingAbsence -> Color.White
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