package fi.kidozz.app.features.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.kidozz.app.ui.components.EventAccordion
import fi.kidozz.app.data.models.CalendarEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviousEventsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: EventViewModel = viewModel()
    val events by viewModel.pastEvents.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Previous Events") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            EventAccordion(
                events = events.toMutableList(),
                onEventUpdated = { updatedEvent ->
                    viewModel.updateEvent(updatedEvent)
                },
                onEventDeleted = { eventToDelete ->
                    viewModel.deleteEvent(eventToDelete.id)
                }
            )
        }
    }
}
