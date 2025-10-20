package fi.kidozz.app.features.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.util.Log
import fi.kidozz.app.data.models.CalendarEvent
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import fi.kidozz.app.data.sample.sampleUpcomingEvents
import fi.kidozz.app.data.sample.samplePastEvents
import fi.kidozz.app.ui.components.EventAccordion
import fi.kidozz.app.ui.components.KiddozzCalendarGrid
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
    var currentMonth by rememberSaveable { mutableStateOf(YearMonth.now()) }
    val context = LocalContext.current
    
    // Add logging for diagnostics
    LaunchedEffect(currentMonth) {
        Log.d("CalendarHeader", "CalendarHeader: month=${currentMonth}")
    }
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

        KiddozzCalendarGrid(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            onDateClick = { selectedDate = it },
            events = allEvents.map { it.date },
            onVisibleMonthChanged = { currentMonth = it }
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


