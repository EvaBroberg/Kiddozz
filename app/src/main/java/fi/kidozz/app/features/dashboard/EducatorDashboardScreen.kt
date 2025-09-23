package fi.kidozz.app.features.dashboard;

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import fi.kidozz.app.data.models.CalendarEvent
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.data.models.Educator
import fi.kidozz.app.data.models.Group
import fi.kidozz.app.core.EducatorSection
import fi.kidozz.app.features.calendar.EducatorCalendarScreen
import fi.kidozz.app.data.sample.sampleUpcomingEvents
import fi.kidozz.app.data.sample.samplePastEvents
import java.time.format.DateTimeFormatter
import androidx.navigation.NavController
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducatorDashboardScreen(
    navController: NavController,
    onBackClick: () -> Unit,
    onKidClick: (Kid) -> Unit, // Assuming Kid is a data class you have defined
    modifier: Modifier = Modifier,
    groupsViewModel: GroupsViewModel,
    educatorViewModel: EducatorViewModel,
    kidsViewModel: KidsViewModel,
    daycareId: String
) {
    // Remember the currently selected section
    var currentEducatorSection by remember { mutableStateOf(EducatorSection.KidsOverview) }
    
    // Filter state for group filtering
    var selectedGroups by remember { mutableStateOf(setOf<String>()) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    
    // Load data from ViewModels
    val groups by groupsViewModel.groups.collectAsState()
    val groupsLoading by groupsViewModel.isLoading.collectAsState()
    val groupsError by groupsViewModel.error.collectAsState()
    
    val currentEducator by educatorViewModel.currentEducator.collectAsState()
    val educatorLoading by educatorViewModel.isLoading.collectAsState()
    val educatorError by educatorViewModel.error.collectAsState()
    
    val kidsList by kidsViewModel.kids.collectAsState()
    val kidsLoading by kidsViewModel.isLoading.collectAsState()
    val kidsError by kidsViewModel.error.collectAsState()
    
    // Load data on first composition
    LaunchedEffect(daycareId) {
        groupsViewModel.loadGroups(daycareId)
        educatorViewModel.loadCurrentEducator(daycareId)
        kidsViewModel.loadKids(daycareId)
    }
    
    // Get available groups from loaded groups
    val availableGroups = groups.map { it.name }.sorted()
    
    // Auto-select educator's groups when data is loaded
    LaunchedEffect(currentEducator, groups) {
        val educator = currentEducator
        if (educator != null && groups.isNotEmpty()) {
            val educatorGroups = educator.groups.map { it.name }
            when {
                educator.role == "super_educator" -> {
                    // Super educator - pre-select ALL available groups
                    selectedGroups = availableGroups.toSet()
                }
                educatorGroups.size == 1 -> {
                    // Single group - auto-select it
                    selectedGroups = setOf(educatorGroups.first())
                }
                educatorGroups.size > 1 -> {
                    // Multiple groups (regular educator) - pre-select all assigned groups
                    selectedGroups = educatorGroups.toSet()
                }
                else -> {
                    // No groups assigned - keep empty selection
                    selectedGroups = emptySet()
                }
            }
        }
    }
    
    // Compute filtered kids based on selected groups
    val filteredKids = remember(kidsList, selectedGroups, groups) {
        if (selectedGroups.isEmpty()) {
            kidsList
        } else {
            kidsList.filter { kid -> 
                // Find the group name for this kid's group_id
                val groupName = groups.find { it.id.toString() == kid.group_id }?.name
                groupName in selectedGroups
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentEducatorSection.title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentEducatorSection == EducatorSection.KidsOverview) {
                        Box {
                            IconButton(onClick = { filterMenuExpanded = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter by group")
                            }
                            DropdownMenu(
                                expanded = filterMenuExpanded,
                                onDismissRequest = { filterMenuExpanded = false }
                            ) {
                                availableGroups.forEach { groupName ->
                                    DropdownMenuItem(
                                        text = { Text(groupName) },
                                        onClick = {
                                            selectedGroups = if (groupName in selectedGroups) {
                                                selectedGroups - groupName
                                            } else {
                                                selectedGroups + groupName
                                            }
                                        },
                                        trailingIcon = {
                                            if (groupName in selectedGroups) {
                                                Checkbox(
                                                    checked = true,
                                                    onCheckedChange = null
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
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
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        innerPadding ->
        // TODO: Replace with actual content for each section
        // For now, using a simple placeholder or the KidsGrid for KidsOverview
        when (currentEducatorSection) {
            EducatorSection.KidsOverview -> KidsGrid(
                filteredKids = filteredKids,
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


