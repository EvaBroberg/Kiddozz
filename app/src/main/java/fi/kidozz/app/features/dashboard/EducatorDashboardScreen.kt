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
import fi.kidozz.app.data.models.CalendarEvent
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.core.EducatorSection
import fi.kidozz.app.features.calendar.EducatorCalendarScreen
import fi.kidozz.app.data.sample.sampleUpcomingEvents
import fi.kidozz.app.data.sample.samplePastEvents
import java.time.format.DateTimeFormatter
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducatorDashboardScreen(
    navController: NavController,
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
            EducatorSection.Calendar -> EducatorCalendarScreen(
                navController = navController,
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
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


