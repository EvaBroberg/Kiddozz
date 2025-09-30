package fi.kidozz.app.features.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.kidozz.app.data.models.CalendarEvent
import androidx.compose.foundation.shape.CircleShape
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import fi.kidozz.app.data.sample.sampleUpcomingEvents
import fi.kidozz.app.data.sample.samplePastEvents
import fi.kidozz.app.ui.components.EventAccordion
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import android.app.Application

@Composable
fun EducatorCalendarScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val viewModel: EventViewModel = viewModel(
        factory = EventViewModelFactory(context.applicationContext as Application)
    )
    
    // Combine both event streams to get all events
    val upcomingEvents by viewModel.upcomingEvents.collectAsState()
    val pastEvents by viewModel.pastEvents.collectAsState()
    val allEvents = remember(upcomingEvents, pastEvents) { 
        upcomingEvents + pastEvents 
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Educator Calendar",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        BasicCalendarView(
            currentMonth = java.time.YearMonth.now(),
            events = allEvents,
            onDateSelected = { selectedDate = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Event lists integrated into calendar view
        if (allEvents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "All Events",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            EventAccordion(
                events = allEvents.toMutableList(),
                modifier = Modifier.fillMaxWidth()
            )
        }



        Spacer(modifier = Modifier.height(16.dp))

        // Add Event button
        Button(
            onClick = { showAddEventDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Add Event",
                style = MaterialTheme.typography.labelLarge
            )
        }

        // Add Event Dialog
        if (showAddEventDialog) {
            AlertDialog(
                onDismissRequest = { showAddEventDialog = false },
                title = { 
                    Text(
                        text = "Add New Event",
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                text = {
                    AddEventForm(
                        onEventAdded = { title, dateTime, location ->
                            val eventDateTime = dateTime 
                            val newEvent = CalendarEvent(
                                title = title,
                                dateTime = eventDateTime,
                                date = eventDateTime.toLocalDate().toString(), 
                                startTime = eventDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")), 
                                description = location 
                            )
                            viewModel.addEvent(newEvent)
                            showAddEventDialog = false
                        }
                    )
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAddEventDialog = false }) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            )
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
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
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
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyMedium
                        )
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
