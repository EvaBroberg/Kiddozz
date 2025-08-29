package fi.kidozz.app.features.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.kidozz.app.CalendarEvent
import java.time.LocalDate
import java.time.YearMonth // Ensured import
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.shape.CircleShape // Was in original, keeping
import fi.kidozz.app.features.calendar.EducatorCalendarScreen

@Composable
fun EducatorCalendarScreen(modifier: Modifier = Modifier) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val events = remember { mutableStateListOf<CalendarEvent>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Educator Calendar",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        BasicCalendarView( // Corrected call
            currentMonth = java.time.YearMonth.now(),
            events = events,
            onDateSelected = { selectedDate = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AddEventForm(
            onEventAdded = { title, dateTime, location ->
                val eventDateTime = dateTime 
                events.add(
                    CalendarEvent(
                        title = title,
                        dateTime = eventDateTime,
                        date = eventDateTime.toLocalDate().toString(), 
                        startTime = eventDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")), 
                        description = location 
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        selectedDate?.let { date ->
            val filteredEvents = events.filter {
                try {
                    java.time.LocalDate.parse(it.date) == date
                } catch (e: Exception) {
                    false
                }
            }
            if (filteredEvents.isNotEmpty()) {
                EventAccordion(events = filteredEvents)
            }
        }
    }
}


@Composable
fun BasicCalendarView(
    currentMonth: java.time.YearMonth,
    events: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1)
    val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.value % 7) 
    val days = (1..daysInMonth).map { currentMonth.atDay(it) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        LazyVerticalGrid(columns = GridCells.Fixed(7)) {
            items(dayOfWeekOffset) { Spacer(modifier = Modifier.size(40.dp)) } // Fill blank days
            items(days) { date ->
                val hasEvent = events.any { it.date == date.toString() }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onDateSelected(date) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = date.dayOfMonth.toString())
                        if (hasEvent) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}
