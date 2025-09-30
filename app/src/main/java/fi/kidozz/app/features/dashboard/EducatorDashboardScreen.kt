package fi.kidozz.app.features.dashboard

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
    onKidClick: (Kid) -> Unit,
    modifier: Modifier = Modifier,
    groupsViewModel: GroupsViewModel,
    educatorViewModel: EducatorViewModel,
    kidsViewModel: KidsViewModel,
    daycareId: String
) {
    // Filter state for group filtering
    var selectedGroups by remember { mutableStateOf(setOf<String>()) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    
    // Load data from ViewModels
    val groups by groupsViewModel.groups.collectAsState()
    val kids by kidsViewModel.kids.collectAsState()
    
    // Load data when screen is first displayed
    LaunchedEffect(daycareId) {
        groupsViewModel.loadGroups(daycareId)
        kidsViewModel.loadKids(daycareId)
    }
    
    // Filter kids based on selected groups
    val filteredKids = remember(kids, selectedGroups) {
        if (selectedGroups.isEmpty()) {
            kids
        } else {
            kids.filter { kid ->
                kid.group_id in selectedGroups
            }
        }
    }
    
    // Get available groups for filter dropdown
    val availableGroups = groups.map { it.name }.distinct().sorted()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kids Overview") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Always show filter for kids overview
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
            )
        },
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { innerPadding ->
        // Show the kids grid directly since navigation is now handled globally
        KidsGrid(
            filteredKids = filteredKids,
            onKidClick = onKidClick,
            modifier = Modifier.padding(innerPadding).padding(top = 24.dp).fillMaxSize()
        )
    }
}
