package fi.kidozz.app.features.dashboard;

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import fi.kidozz.app.CalendarEvent
import fi.kidozz.app.Kid // Added import for Kid
import fi.kidozz.app.EducatorSection // Added import for EducatorSection
import fi.kidozz.app.features.calendar.EducatorCalendarScreen
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducatorDashboardScreen(
    onBackClick: () -> Unit,
    onKidClick: (Kid) -> Unit, // Assuming Kid is a data class you have defined
    modifier: Modifier = Modifier,
    kidsList: List<Kid> // Assuming Kid is a data class you have defined
) {
    // Remember the currently selected section
    var currentEducatorSection by remember { mutableStateOf(EducatorSection.KidsOverview) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentEducatorSection.title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                EducatorSection.values().forEach { section ->
                    NavigationBarItem(
                        icon = { Icon(section.icon, contentDescription = section.title) },
                        label = { Text(section.title) },
                        selected = currentEducatorSection == section,
                        onClick = { currentEducatorSection = section }
                    )
                }
            }
        },
        modifier = modifier
    ) {
        innerPadding ->
        // TODO: Replace with actual content for each section
        // For now, using a simple placeholder or the KidsGrid for KidsOverview
        when (currentEducatorSection) {
            EducatorSection.KidsOverview -> KidsGrid(
                kids = kidsList,
                onKidClick = onKidClick,
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
            EducatorSection.Calendar -> EducatorCalendarScreen(modifier = Modifier.padding(innerPadding).fillMaxSize())
            else -> PlaceholderScreen(
                section = currentEducatorSection,
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
        }
    }
}

@Composable
fun PlaceholderScreen(section: EducatorSection, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Content for ${section.title}", style = MaterialTheme.typography.headlineMedium)
    }
}

// Existing EventAccordion and EventAccordionItem code from the file
@Composable
fun EventAccordion(events: List<CalendarEvent>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        events.forEach { event ->
            EventAccordionItem(event = event)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun EventAccordionItem(event: CalendarEvent) {
    var expanded by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Date: ${event.dateTime.format(formatter)}")
                Text(text = "Location info unavailable") // Placeholder for location
            }
        }
    }
}
