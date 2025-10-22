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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.ui.components.KidAccordionCard
import fi.kidozz.app.ui.components.AbsenceCalendarDialog
import fi.kidozz.app.ui.styles.WarningButton
import fi.kidozz.app.ui.theme.KiddozzTheme
import fi.kidozz.app.ui.theme.InCareColor
import fi.kidozz.app.ui.theme.OutColor
import fi.kidozz.app.ui.theme.AbsenceBackgroundColor
import fi.kidozz.app.ui.theme.AbsenceTextColor
import fi.kidozz.app.ui.theme.SickAbsenceBackgroundColor
import fi.kidozz.app.ui.styles.AppColors
import kotlinx.coroutines.launch
import fi.kidozz.app.ui.theme.SickColor
import fi.kidozz.app.ui.theme.HolidayColor
import fi.kidozz.app.ui.components.ScrollablePage
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    parentId: String,
    parentsViewModel: ParentsViewModel,
    absenceReasonsViewModel: AbsenceReasonsViewModel,
    kidsRepository: fi.kidozz.app.data.repository.KidsRepository,
    modifier: Modifier = Modifier
) {
    val kids by parentsViewModel.kids.collectAsState()
    val isLoading by parentsViewModel.isLoading.collectAsState()
    val error by parentsViewModel.error.collectAsState()
    val absenceReasons by absenceReasonsViewModel.absenceReasons.collectAsState()
    
    // Calendar dialog state
    var showAbsenceCalendar by remember { mutableStateOf(false) }
    var selectedKid by remember { mutableStateOf<Kid?>(null) }
    
    // Coroutine scope for absence submission
    val coroutineScope = rememberCoroutineScope()
    
    // Load kids when screen is first displayed
    LaunchedEffect(parentId) {
        parentsViewModel.loadKidsForParent(parentId)
        absenceReasonsViewModel.loadAbsenceReasons()
    }
    
    // Refresh data when screen resumes (lifecycle-aware)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        snapshotFlow { lifecycleOwner.lifecycle.currentState }
            .filter { it == Lifecycle.State.RESUMED }
            .collect {
                parentsViewModel.refreshKids(parentId)
            }
    }
    
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { innerPadding ->
                ScrollablePage(
                    innerPadding = innerPadding
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        kids.forEach { kid ->
                            Column {
                                // Compute effective attendance for today, prioritizing today's absence reason
                                val effectiveAttendance by produceState(initialValue = kid.attendance, key1 = kid.id) {
                                    val today = java.time.LocalDate.now().toString()
                                    val abs = kidsRepository.getAbsences(kid.id).getOrNull()

                                    // Default
                                    var derived = kid.attendance

                                    if (abs != null) {
                                        // Find any absence for today
                                        val todays = abs.firstOrNull { (it["date"] as? String) == today }
                                        val reason = todays?.get("reason") as? String

                                        derived = when (reason?.lowercase()) {
                                            "sick" -> "sick"          // highest priority
                                            "holiday" -> "holiday"
                                            else -> kid.attendance
                                        }
                                    }

                                    value = derived
                                    println("ParentDashboard: ${kid.full_name} today=$today → $derived (was ${kid.attendance})")
                                }

                                // Determine status color (including holiday)
                                val statusColor = when (effectiveAttendance.lowercase()) {
                                    "in-care" -> InCareColor
                                    "out" -> OutColor
                                    "sick" -> SickColor
                                    "holiday" -> HolidayColor
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
                                        status = effectiveAttendance,
                                        onChatClick = { /* TODO: Implement chat functionality */ },
                                        expandedContent = {
                                            // Absence status messages
                                            val absenceMessages = getAbsenceMessages(kid, kidsRepository)
                                            if (absenceMessages.isNotEmpty()) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    absenceMessages.forEachIndexed { index, message ->
                                                        val isSickLeave = message.contains("RED_START")
                                                        val cardColor = if (isSickLeave) SickAbsenceBackgroundColor else AbsenceBackgroundColor
                                                        
                                                        Card(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(bottom = if (index < absenceMessages.size - 1) 8.dp else 0.dp),
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = cardColor
                                                            ),
                                                            shape = MaterialTheme.shapes.small
                                                        ) {
                                                            Column(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(12.dp)
                                                            ) {
                                                                StyledAbsenceText(
                                                                    text = message.uppercase(),
                                                                    style = MaterialTheme.typography.bodyLarge,
                                                                    fontWeight = FontWeight.Medium,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            WarningButton(
                                                text = "Report Absence",
                                                onClick = { 
                                                    selectedKid = kid
                                                    showAbsenceCalendar = true
                                                },
                                                modifier = Modifier.padding(top = 16.dp)
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
                onAbsenceSelected = { selectedDates, reason, details ->
                    if (selectedDates.isEmpty()) {
                        // optional mild feedback; do not crash
                        println("Absence submit skipped: no new dates")
                        return@AbsenceCalendarDialog
                    }
                    
                    // Submit absence to backend
                    coroutineScope.launch {
                        try {
                            val dateStrings = selectedDates.map { it.toString() }
                            println("ParentDashboardScreen: Submitting absence for ${kid.full_name}")
                            println("ParentDashboardScreen: Dates: $dateStrings, Reason: $reason, Details: $details")
                            
                            val result = kidsRepository.submitAbsence(
                                kidId = kid.id,
                                dates = dateStrings,
                                reason = reason,
                                details = details
                            )
                            
                            if (result.isSuccess) {
                                println("ParentDashboardScreen: Successfully submitted absence")
                                // Refresh the kids list to show updated absence messages
                                parentsViewModel.loadKidsForParent(parentId)
                            } else {
                                println("ParentDashboardScreen: Failed to submit absence: ${result.exceptionOrNull()?.message}")
                            }
                        } catch (e: Exception) {
                            println("ParentDashboardScreen: Exception during absence submission: ${e.message}")
                        }
                    }
                },
                kidName = kid.full_name,
                kidId = kid.id,
                kidsRepository = kidsRepository,
                absenceReasons = absenceReasons
            )
        }
    }
}

/**
 * Generates multiple absence messages for a kid based on their absence records.
 * 
 * @param kid The kid to generate the messages for
 * @return A list of formatted absence messages
 */
@Composable
private fun getAbsenceMessages(kid: Kid, kidsRepository: fi.kidozz.app.data.repository.KidsRepository): List<String> {
    var absenceMessages by remember { mutableStateOf<List<String>>(emptyList()) }
    
    LaunchedEffect(kid.id) {
        try {
            val result = kidsRepository.getAbsences(kid.id.toString())
            if (result.isSuccess) {
                val absences = result.getOrNull() ?: emptyList()
                val today = LocalDate.now()
                val messages = mutableListOf<String>()
                
                // Group absences by reason and date
                val futureAbsences = absences.filter { absence ->
                    val absenceDate = LocalDate.parse(absence["date"] as String)
                    absenceDate.isAfter(today) || absenceDate.isEqual(today)
                }.sortedBy { LocalDate.parse(it["date"] as String) }
                
                if (futureAbsences.isNotEmpty()) {
                    val sickAbsences = futureAbsences.filter { it["reason"] == "sick" }
                    val holidayAbsences = futureAbsences.filter { it["reason"] == "holiday" }
                    
                    // Process sick absences
                    if (sickAbsences.isNotEmpty()) {
                        val sickDates = sickAbsences.map { LocalDate.parse(it["date"] as String) }
                        val sickMessage = createStyledAbsenceMessage(kid.full_name, "sick leave", sickDates)
                        if (sickMessage.isNotEmpty()) {
                            messages.add(sickMessage)
                        }
                    }
                    
                    // Process holiday absences
                    if (holidayAbsences.isNotEmpty()) {
                        val holidayDates = holidayAbsences.map { LocalDate.parse(it["date"] as String) }
                        val holidayMessage = createStyledAbsenceMessage(kid.full_name, "holiday", holidayDates)
                        if (holidayMessage.isNotEmpty()) {
                            messages.add(holidayMessage)
                        }
                    }
                }
                
                absenceMessages = messages
            } else {
                println("Failed to fetch absences for ${kid.full_name}: ${result.exceptionOrNull()?.message}")
                absenceMessages = emptyList()
            }
        } catch (e: Exception) {
            println("Exception fetching absences for ${kid.full_name}: ${e.message}")
            absenceMessages = emptyList()
        }
    }
    
    return absenceMessages
}


/**
 * Formats absence dates into a readable message with range compression.
 * Groups consecutive days into ranges for better readability.
 */
private fun formatAbsenceMessage(kidName: String, dates: List<LocalDate>, reason: String): String {
    if (dates.isEmpty()) return ""
    
    val today = LocalDate.now()
    val futureDates = dates.filter { it.isAfter(today) || it.isEqual(today) }
    if (futureDates.isEmpty()) return ""
    
    val sortedDates = futureDates.sorted()
    val verb = when (reason) {
        "holiday" -> "on holiday"
        "sick" -> "on sick leave"
        else -> "away"
    }
    
    return formatAbsenceMessageBulleted(kidName, verb, sortedDates)
}

/**
 * Formats absence dates with range compression for consecutive days.
 * 
 * @param kidName The name of the child
 * @param absenceType The type of absence (e.g., "on holiday", "away")
 * @param dates Sorted list of absence dates
 * @return Formatted message with compressed ranges
 */
private fun formatAbsenceMessageWithRanges(
    kidName: String,
    absenceType: String,
    dates: List<LocalDate>
): String {
    if (dates.isEmpty()) return "$kidName has no recorded absences."

    val formatter = DateTimeFormatter.ofPattern("MMM dd")
    val sortedDates = dates.sorted()

    val ranges = mutableListOf<String>()
    var rangeStart = sortedDates.first()
    var prev = sortedDates.first()

    for (i in 1 until sortedDates.size) {
        val current = sortedDates[i]
        if (current == prev.plusDays(1)) {
            // still in a consecutive streak
            prev = current
        } else {
            // close the range
            ranges.add(
                if (rangeStart == prev) {
                    rangeStart.format(formatter)
                } else {
                    "from ${rangeStart.format(formatter)} to ${prev.format(formatter)}"
                }
            )
            rangeStart = current
            prev = current
        }
    }

    // close the last range
    ranges.add(
        if (rangeStart == prev) {
            rangeStart.format(formatter)
        } else {
            "from ${rangeStart.format(formatter)} to ${prev.format(formatter)}"
        }
    )

    val joined = ranges.joinToString(", ")
    return "$kidName is $absenceType $joined"
}

/**
 * Formats absence dates with range compression in a bulleted multi-line format.
 * 
 * @param kidName The name of the child
 * @param absenceType The type of absence (e.g., "holiday", "sick")
 * @param dates Sorted list of absence dates
 * @return Formatted message with bulleted ranges
 */
private fun formatAbsenceMessageBulleted(
    kidName: String,
    absenceType: String,
    dates: List<LocalDate>
): String {
    if (dates.isEmpty()) return "$kidName has no recorded absences."

    val formatter = DateTimeFormatter.ofPattern("MMM dd")
    val sortedDates = dates.sorted()

    val ranges = mutableListOf<String>()
    var rangeStart = sortedDates.first()
    var prev = sortedDates.first()

    for (i in 1 until sortedDates.size) {
        val current = sortedDates[i]
        if (current == prev.plusDays(1)) {
            prev = current
        } else {
            ranges.add(
                if (rangeStart == prev) {
                    "• ${rangeStart.format(formatter)}"
                } else {
                    "• from ${rangeStart.format(formatter)} to ${prev.format(formatter)}"
                }
            )
            rangeStart = current
            prev = current
        }
    }

    // Close last range
    ranges.add(
        if (rangeStart == prev) {
            "• ${rangeStart.format(formatter)}"
        } else {
            "• from ${rangeStart.format(formatter)} to ${prev.format(formatter)}"
        }
    )

    return buildString {
        append("$kidName is on $absenceType:\n")
        ranges.forEach { appendLine(it) }
    }
}

/**
 * Creates styled absence message with different colors for different parts.
 * Uses special markers for styling that can be parsed in the UI.
 * 
 * @param kidName The name of the child
 * @param absenceType The type of absence (e.g., "holiday", "sick leave")
 * @param dates Sorted list of absence dates
 * @return String with special markers for styling
 */
private fun createStyledAbsenceMessage(
    kidName: String,
    absenceType: String,
    dates: List<LocalDate>
): String {
    if (dates.isEmpty()) {
        return "$kidName has no recorded absences."
    }

    val formatter = DateTimeFormatter.ofPattern("MMM dd")
    val sortedDates = dates.sorted()

    val ranges = mutableListOf<String>()
    var rangeStart = sortedDates.first()
    var prev = sortedDates.first()

    for (i in 1 until sortedDates.size) {
        val current = sortedDates[i]
        if (current == prev.plusDays(1)) {
            prev = current
        } else {
            ranges.add(
                if (rangeStart == prev) {
                    "• ${rangeStart.format(formatter)}"
                } else {
                    "• from ${rangeStart.format(formatter)} to ${prev.format(formatter)}"
                }
            )
            rangeStart = current
            prev = current
        }
    }

    // Close last range
    ranges.add(
        if (rangeStart == prev) {
            "• ${rangeStart.format(formatter)}"
        } else {
            "• from ${rangeStart.format(formatter)} to ${prev.format(formatter)}"
        }
    )

    return buildString {
        when (absenceType) {
            "holiday" -> append("YELLOW_START$kidName is on $absenceType:YELLOW_END\n\n")
            "sick leave" -> append("RED_START$kidName is on $absenceType:RED_END\n\n")
            else -> append("$kidName is on $absenceType:\n\n")
        }
        ranges.forEach { appendLine(it) }
    }
}

/**
 * Composable that displays styled absence text with different colors for different parts.
 * Parses special markers in the text to apply yellow color to holiday text.
 */
@Composable
private fun StyledAbsenceText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier
) {
    val annotatedString = buildAnnotatedString {
        // Handle yellow markers first
        if (text.contains("YELLOW_START") && text.contains("YELLOW_END")) {
            val yellowParts = text.split("YELLOW_START", "YELLOW_END")
            for (i in yellowParts.indices) {
                val part = yellowParts[i]
                if (i == 1) {
                    // This is the part between YELLOW_START and YELLOW_END (holiday sentence)
                    withStyle(style = SpanStyle(color = AbsenceTextColor, fontWeight = fontWeight)) {
                        append(part)
                    }
                } else {
                    // Regular text in black with lighter font weight for bullet points
                    withStyle(style = SpanStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Normal
                    )) {
                        append(part)
                    }
                }
            }
        } else if (text.contains("RED_START") && text.contains("RED_END")) {
            // Handle red markers
            val redParts = text.split("RED_START", "RED_END")
            for (i in redParts.indices) {
                val part = redParts[i]
                if (i == 1) {
                    // This is the part between RED_START and RED_END (sick leave sentence)
                    withStyle(style = SpanStyle(color = AppColors.Error, fontWeight = fontWeight)) {
                        append(part)
                    }
                } else {
                    // Regular text in black with lighter font weight for bullet points
                    withStyle(style = SpanStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Normal
                    )) {
                        append(part)
                    }
                }
            }
        } else {
            // No markers, just regular text
            withStyle(style = SpanStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
            )) {
                append(text)
            }
        }
    }
    
    Text(
        text = annotatedString,
        style = style,
        lineHeight = style.fontSize * 1.2, // Slightly increased line height for better spacing
        modifier = modifier
    )
}

/**
 * Generates an absence message for a kid based on their current absence status.
 * 
 * @param kid The kid to generate the message for
 * @return A formatted absence message, or empty string if no absence
 */
private fun getAbsenceMessage(kid: Kid): String {
    // For demonstration purposes, let's show absence messages for some kids
    // In a real implementation, this would fetch actual absence data from the backend
    
    // Simulate some absence data for demonstration
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val dayAfterTomorrow = today.plusDays(2)
    
    // Create sample absence scenarios for different kids with different reasons
    return when (kid.full_name) {
        "Liam Johnson" -> {
            // Simulate sick absence - consecutive days
            "${kid.full_name} is away until ${tomorrow.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
        }
        "Emma Johnson" -> {
            // Simulate sick absence - single day
            "${kid.full_name} is away on ${today.format(DateTimeFormatter.ofPattern("MMM dd"))}"
        }
        "Sophia Smith" -> {
            // Simulate holiday absence - non-consecutive days (Monday and Wednesday)
            val monday = today.plusDays(1) // Next Monday
            val wednesday = today.plusDays(3) // Next Wednesday
            "${kid.full_name} is on holiday on ${monday.format(DateTimeFormatter.ofPattern("MMM dd"))}, ${wednesday.format(DateTimeFormatter.ofPattern("MMM dd"))}"
        }
        "Olivia Wilson" -> {
            // Simulate holiday absence - longer period
            "${kid.full_name} is on holiday until ${dayAfterTomorrow.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
        }
        else -> {
            // For other kids, show message based on their attendance status
            when (kid.attendance) {
                "sick" -> "${kid.full_name} is away until ${tomorrow.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
                "holiday" -> "${kid.full_name} is on holiday until ${tomorrow.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
                else -> ""
            }
        }
    }
}

/**
 * Generates a more sophisticated absence message that handles multiple days and gaps.
 * This would be used when we have real absence data from the backend.
 * 
 * @param kid The kid to generate the message for
 * @param absenceDates List of absence dates for the kid
 * @param reason The reason for absence ("sick" or "holiday")
 * @return A formatted absence message, or empty string if no absence
 */
private fun getAdvancedAbsenceMessage(kid: Kid, absenceDates: List<LocalDate>, reason: String = "sick"): String {
    if (absenceDates.isEmpty()) return ""
    
    val today = LocalDate.now()
    val futureAbsences = absenceDates.filter { it.isAfter(today) || it.isEqual(today) }
    
    if (futureAbsences.isEmpty()) return ""
    
    val sortedAbsences = futureAbsences.sorted()
    
    // Choose the appropriate verb based on the reason
    val verb = when (reason) {
        "holiday" -> "on holiday"
        "sick" -> "on sick leave"
        else -> "away"
    }
    
    return formatAbsenceMessageBulleted(kid.full_name, verb, sortedAbsences)
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
