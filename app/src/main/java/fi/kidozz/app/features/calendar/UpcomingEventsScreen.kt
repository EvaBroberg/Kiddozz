package fi.kidozz.app.features.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.navigation.NavController
import fi.kidozz.app.ui.components.EventAccordion
import fi.kidozz.app.ui.components.ScrollablePage
import java.time.LocalDate

@Composable
fun UpcomingEventsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: EventViewModel = viewModel(
        factory = EventViewModelFactory(context.applicationContext as Application)
    )
    
    val upcomingEvents by viewModel.upcomingEvents.collectAsState()
    val today = LocalDate.now()
    val filteredEvents = remember(upcomingEvents, today) {
        upcomingEvents.filter { event ->
            try {
                val eventDate = LocalDate.parse(event.date)
                eventDate >= today
            } catch (e: Exception) {
                false
            }
        }
    }

    ScrollablePage(
        modifier = modifier
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            
            Text(
                text = "Upcoming Events",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (filteredEvents.isEmpty()) {
            Text(
                text = "No upcoming events",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            EventAccordion(
                events = filteredEvents.toMutableList(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
