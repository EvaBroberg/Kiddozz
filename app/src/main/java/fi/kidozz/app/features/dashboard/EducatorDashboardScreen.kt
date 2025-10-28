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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filter
import fi.kidozz.app.data.models.CalendarEvent
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.data.models.Educator
import fi.kidozz.app.data.models.Group
import fi.kidozz.app.data.sample.sampleUpcomingEvents
import fi.kidozz.app.data.sample.samplePastEvents
import fi.kidozz.app.ui.components.LazyPage
import java.time.format.DateTimeFormatter
import androidx.navigation.NavController
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducatorDashboardScreen(
    onSelectKidsOverview: () -> Unit,
    onSelectCalendar: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    groupsViewModel: GroupsViewModel? = null,
    educatorViewModel: EducatorViewModel? = null,
    kidsViewModel: KidsViewModel? = null,
    daycareId: String = "default-daycare-id"
) {
    var filterMenuExpanded by remember { mutableStateOf(false) }
    
    // Load data from ViewModels
    val groups by groupsViewModel?.groups?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val selectedGroupIds by kidsViewModel?.selectedGroupIds?.collectAsState() ?: remember { mutableStateOf(emptySet()) }
    val currentEducator by educatorViewModel?.currentEducator?.collectAsState() ?: remember { mutableStateOf(null) }
    
    // Load data when screen is first displayed
    LaunchedEffect(daycareId) {
        groupsViewModel?.loadGroups(daycareId)
        kidsViewModel?.loadKids(daycareId)
        educatorViewModel?.loadCurrentEducatorByDaycare(daycareId)
    }
    
    // Initialize selectedGroupIds with educator's assigned groups (only when empty)
    LaunchedEffect(currentEducator) {
        val educator = currentEducator
        if (educator != null && kidsViewModel != null) {
            val educatorGroupIds = educator.groups.map { it.id }.toSet()
            val currentSelected = kidsViewModel.selectedGroupIds.value
            if (currentSelected.isEmpty()) {
                kidsViewModel.setSelectedGroupIds(educatorGroupIds)
                android.util.Log.d("EducatorFilter", "educator=${educator.full_name} groupIds=${educatorGroupIds}")
                android.util.Log.d("EducatorFilter", "selectedGroups(default)=${educatorGroupIds}")
            }
        }
    }
    
    // Refresh data when screen resumes (lifecycle-aware)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        snapshotFlow { lifecycleOwner.lifecycle.currentState }
            .filter { it == Lifecycle.State.RESUMED }
            .collect {
                kidsViewModel?.refreshKids(daycareId)
            }
    }
    
    // Get available groups for filter dropdown (keep full Group objects)
    val availableGroups = groups.sortedBy { it.name }
    
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Use the content parameter instead of hardcoded KidsGrid
            Box(
                modifier = Modifier.padding(innerPadding)
            ) {
                content()
            }
            
            // Sticky filter bar at top (only show for kids overview)
            if (groupsViewModel != null && educatorViewModel != null && kidsViewModel != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Box {
                        IconButton(
                            onClick = { filterMenuExpanded = true }
                        ) {
                            Icon(
                                Icons.Default.FilterList, 
                                contentDescription = "Filter by group",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        DropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false }
                        ) {
                            availableGroups.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text(group.name) },
                                    onClick = {
                                        kidsViewModel?.toggleGroup(group.id)
                                        android.util.Log.d("EducatorFilter", "toggled ${group.id}(${group.name})")
                                    },
                                    trailingIcon = {
                                        if (group.id in selectedGroupIds) {
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
        }
    }
}

@Composable
fun EducatorDashboardContent(
    groupsViewModel: GroupsViewModel,
    educatorViewModel: EducatorViewModel,
    kidsViewModel: KidsViewModel,
    daycareId: String,
    onKidClick: (String) -> Unit
) {
    // Load data from ViewModels
    val groups by groupsViewModel.groups.collectAsState()
    val filteredKids by kidsViewModel.filteredKids.collectAsState()
    val currentEducator by educatorViewModel.currentEducator.collectAsState()
    
    // Load data when screen is first displayed
    LaunchedEffect(daycareId) {
        groupsViewModel.loadGroups(daycareId)
        kidsViewModel.loadKids(daycareId)
        educatorViewModel.loadCurrentEducatorByDaycare(daycareId)
    }
    
    // Initialize selectedGroupIds with educator's assigned groups (only when empty)
    LaunchedEffect(currentEducator) {
        val educator = currentEducator
        if (educator != null) {
            val educatorGroupIds = educator.groups.map { it.id }.toSet()
            val currentSelected = kidsViewModel.selectedGroupIds.value
            if (currentSelected.isEmpty()) {
                kidsViewModel.setSelectedGroupIds(educatorGroupIds)
                android.util.Log.d("EducatorFilter", "educator=${educator.full_name} groupIds=${educatorGroupIds}")
                android.util.Log.d("EducatorFilter", "selectedGroups(default)=${educatorGroupIds}")
            }
        }
    }
    
    // Show the kids grid
    fi.kidozz.app.ui.components.LazyPage {
        KidsGrid(
            kids = filteredKids,
            onKidClick = onKidClick
        )
    }
}
