package fi.kidozz.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import android.util.Log

@Composable
fun KiddozzCalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    showWeekDays: Boolean = true,
    showMonthHeader: Boolean = true, // Control whether to show month header
    events: List<String> = emptyList(), // List of event dates as strings
    selectedDates: Set<LocalDate> = emptySet(), // For multi-select scenarios
    existingAbsences: Map<LocalDate, String> = emptyMap(), // For absence calendar
    isMultiSelect: Boolean = false, // Whether to support multi-selection
    onDateSelected: ((LocalDate) -> Unit)? = null, // Alternative callback for multi-select
    onVisibleMonthChanged: ((YearMonth) -> Unit)? = null // Callback when visible month changes
) {
    val monthRange = remember(currentMonth) {
        // Generate ±6 months around current month
        (-6..6).map { currentMonth.plusMonths(it.toLong()) }
    }
    val listState = rememberLazyListState()
    
    // Add logging for diagnostics
    LaunchedEffect(currentMonth) {
        Log.d("CalendarGrid", "CalendarGrid: month=${currentMonth}")
    }
    

    // Track the currently visible month for the header
    var visibleMonth by remember { mutableStateOf(currentMonth) }
    
    // Update visible month when scroll position changes
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex >= 0 && listState.firstVisibleItemIndex < monthRange.size) {
            visibleMonth = monthRange[listState.firstVisibleItemIndex]
            onVisibleMonthChanged?.invoke(visibleMonth)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Single month header that updates with visible month (only if enabled)
        if (showMonthHeader) {
            Text(
                text = visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(8.dp))
        }

        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = (7 * 48 + 6 * 8 + 32).dp)
                .testTag("calendar_grid"),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            flingBehavior = rememberSnapFlingBehavior(
                lazyListState = listState,
                snapPosition = SnapPosition.Center
            )
        ) {
        items(monthRange.size) { index ->
            val month = monthRange[index]
            Column(
                modifier = Modifier
                    .width(350.dp) // increased width to prevent cutoff
            ) {

                // Week day headers
                if (showWeekDays) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
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
                    Spacer(Modifier.height(4.dp))
                }

                val daysInMonth = remember(month) {
                    (1..month.lengthOfMonth()).map { 
                        LocalDate.of(month.year, month.month, it)
                    }
                }

                val firstDayOfMonth = month.atDay(1)
                val offset = (firstDayOfMonth.dayOfWeek.value % 7)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    userScrollEnabled = false, // prevent nested vertical scroll
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(offset) {
                        Spacer(modifier = Modifier.size(32.dp))
                    }

                    items(daysInMonth.size) { dayIndex ->
                        val date = daysInMonth[dayIndex]
                        val isSelected = if (isMultiSelect) {
                            date in selectedDates
                        } else {
                            date == selectedDate
                        }
                        val isToday = date == LocalDate.now()
                        val isPast = date.isBefore(LocalDate.now())
                        val hasEvent = events.contains(date.toString())
                        val hasExistingAbsence = existingAbsences.containsKey(date)
                        val existingAbsenceReason = existingAbsences[date]
                        
                        // Determine if date should be clickable
                        val isClickable = when {
                            isMultiSelect -> !isPast && !hasExistingAbsence
                            else -> true
                        }

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
                                .clickable(enabled = isClickable) {
                                    if (isMultiSelect && onDateSelected != null) {
                                        onDateSelected(date)
                                    } else {
                                        onDateClick(date)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
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
                                
                                // Show event indicator for single-select mode
                                if (!isMultiSelect && hasEvent) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(
                                                if (isSelected) Color.White 
                                                else MaterialTheme.colorScheme.primary, 
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}
