package fi.kidozz.app.features.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.kidozz.app.ui.components.EventAccordion
import fi.kidozz.app.data.models.CalendarEvent
import android.app.Application
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviousEventsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: EventViewModel = viewModel(
        factory = EventViewModelFactory(context.applicationContext as Application)
    )
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
        },
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .padding(top = 24.dp)
                .verticalScroll(rememberScrollState())
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
