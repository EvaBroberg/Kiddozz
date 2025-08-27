package fi.kidozz.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems // Alias for grid items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.kidozz.app.ui.theme.KiddozzTheme
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KiddozzTheme {
                KiddozzAppHost()
            }
        }
    }
}

enum class Screen {
    ROLE_SELECTION,
    EDUCATOR_DASHBOARD,
    KID_DETAIL
}

enum class EducatorSection(val title: String, val icon: ImageVector) {
    KidsOverview("Kids Overview", Icons.Filled.Face),
    Calendar("Calendar", Icons.Filled.DateRange),
    Events("Events", Icons.Filled.Face),
    Menu("Menu", Icons.Filled.Face),
    Profile("Profile", Icons.Filled.Person)
}

enum class CalendarDisplayMode {
    GRID,
    UPCOMING_LIST,
    PAST_LIST
}

data class Guardian(
    val name: String,
    val email: String,
    val phone: String,
    val relationship: String
)

data class Kid(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    var attendanceStatus: String = "OUT",
    val profileImageUrl: String? = null,
    val allergies: List<String> = emptyList(),
    val needToKnow: String = "",
    val primaryGuardian: Guardian,
    val secondaryGuardian: Guardian? = null,
    val authorizedPickups: List<Guardian> = emptyList(),
    val address: String = ""
)

data class CalendarEvent(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var date: String = "",
    var startTime: String = "",
    var endTime: String = "",
    var isAllDay: Boolean = false,
    var description: String = "",
    var imageUris: List<String> = emptyList(),
    var isPast: Boolean = false
)

val sampleKidsState = mutableStateListOf<Kid>().apply {
    addAll(
        List(10) { index ->
            Kid(
                name = "Kid ${index + 1}",
                attendanceStatus = if (index % 3 == 0) "IN" else if (index % 3 == 1) "SICK" else "OUT",
                allergies = if (index % 2 == 0) listOf("Peanuts", "Dairy") else emptyList(),
                needToKnow = if (index % 4 == 0) "Needs afternoon nap at 1 PM" else "",
                primaryGuardian = Guardian("Parent ${index + 1}A", "parent${index+1}a@example.com", "555-010${index}A", "Mother"),
                secondaryGuardian = if (index % 3 == 0) Guardian("Parent ${index + 1}B", "parent${index+1}b@example.com", "555-010${index}B", "Father") else null,
                authorizedPickups = if (index % 2 != 0) listOf(Guardian("Grandma ${index+1}", "grandma${index+1}@example.com", "555-020${index}", "Grandmother")) else emptyList(),
                address = "${index + 1} Sample Street, Kidtown"
            )
        }
    )
}

val sampleUpcomingEvents = mutableStateListOf(
    CalendarEvent(title = "Spring Festival", date = "2024-05-10", description = "Join us for a fun-filled day of games, food, and music to celebrate spring!", imageUris = listOf("spring_banner.png", "kids_playing.jpg"), isPast = false),
    CalendarEvent(title = "Parent-Teacher Meeting", date = "2024-05-15", description = "Discuss your child's progress with their teachers. Sign up for a slot!", imageUris = emptyList(), isPast = false),
    CalendarEvent(title = "Art Workshop", date = "2024-05-22", description = "Creative workshop for all age groups. Materials provided.", imageUris = listOf("art_supplies.png"), isPast = false)
)

val samplePastEvents = mutableStateListOf(
    CalendarEvent(title = "Book Fair", date = "2024-03-01", description = "Successful book fair. Thank you all for participating!", imageUris = listOf("book_fair_photo1.jpg"), isPast = true),
    CalendarEvent(title = "Sports Day", date = "2024-02-15", description = "A wonderful day of sports and teamwork.", imageUris = listOf("sports_day_group.png", "medal_ceremony.jpg"), isPast = true)
)

@Composable
fun KiddozzAppHost(modifier: Modifier = Modifier) {
    var currentScreen by remember { mutableStateOf(Screen.ROLE_SELECTION) }
    var selectedKid by remember { mutableStateOf<Kid?>(null) }

    when (currentScreen) {
        Screen.ROLE_SELECTION -> RoleSelectionScreen(
            onEducatorViewClick = { currentScreen = Screen.EDUCATOR_DASHBOARD },
            onParentViewClick = { /* TODO: Navigate to Parent View */ },
            modifier = modifier
        )
        Screen.EDUCATOR_DASHBOARD -> EducatorDashboardScreen(
            onBackClick = { currentScreen = Screen.ROLE_SELECTION },
            onKidClick = { kid ->
                selectedKid = kid
                currentScreen = Screen.KID_DETAIL
            },
            modifier = modifier,
            kidsList = sampleKidsState
        )
        Screen.KID_DETAIL -> {
            val kid = selectedKid
            if (kid != null) {
                KidDetailScreen(
                    kid = kid,
                    onBackClick = {
                        currentScreen = Screen.EDUCATOR_DASHBOARD
                        selectedKid = null
                    },
                    onAttendanceStatusChange = { newStatus ->
                        val kidIndex = sampleKidsState.indexOfFirst { it.id == kid.id }
                        if (kidIndex != -1) {
                            sampleKidsState[kidIndex] = sampleKidsState[kidIndex].copy(attendanceStatus = newStatus)
                        }
                        selectedKid = selectedKid?.copy(attendanceStatus = newStatus)
                    }
                )
            } else {
                currentScreen = Screen.EDUCATOR_DASHBOARD
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    onEducatorViewClick: () -> Unit,
    onParentViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Kiddozz Daycare App") }) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Welcome to Kiddozz!")
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onEducatorViewClick) { Text("Educator View") }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onParentViewClick) { Text("Parent View") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducatorDashboardScreen(
    onBackClick: () -> Unit,
    onKidClick: (Kid) -> Unit,
    kidsList: List<Kid>,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentEducatorSection by remember { mutableStateOf(EducatorSection.KidsOverview) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                EducatorSection.values().forEach { section ->
                    NavigationDrawerItem(
                        icon = { Icon(section.icon, contentDescription = section.title) },
                        label = { Text(section.title) },
                        selected = section == currentEducatorSection,
                        onClick = {
                            scope.launch { drawerState.close() }
                            currentEducatorSection = section
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                NavigationDrawerItem(
                    label = { Text("Back to Role Selection") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onBackClick()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Role Selection")}
                )
                Spacer(Modifier.height(12.dp))
            }
        },
        modifier = modifier
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentEducatorSection.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { if (drawerState.isClosed) drawerState.open() else drawerState.close() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open Navigation Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            when (currentEducatorSection) {
                EducatorSection.KidsOverview -> KidsGrid(
                    kids = kidsList,
                    onKidClick = onKidClick,
                    modifier = Modifier.padding(innerPadding).fillMaxSize()
                )
                EducatorSection.Calendar -> EducatorCalendarScreen(modifier = Modifier.padding(innerPadding))
                EducatorSection.Events -> PlaceholderScreen(section = currentEducatorSection, modifier = Modifier.padding(innerPadding))
                EducatorSection.Menu -> PlaceholderScreen(section = currentEducatorSection, modifier = Modifier.padding(innerPadding))
                EducatorSection.Profile -> PlaceholderScreen(section = currentEducatorSection, modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
fun KidsGrid(kids: List<Kid>, onKidClick: (Kid) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        gridItems(kids, key = { it.id }) { kid ->
            KiddozCard(kid = kid, onClick = { onKidClick(kid) })
        }
    }
}

@Composable
fun KiddozCard(kid: Kid, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().aspectRatio(1f),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Face, contentDescription = "Kid icon", modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = kid.name, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            }
            Text(
                text = kid.attendanceStatus.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                color = if (kid.attendanceStatus == "SICK") Color.Black else Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when (kid.attendanceStatus) {
                            "IN" -> Color.Green.copy(alpha = 0.7f)
                            "SICK" -> Color.Yellow
                            else -> Color.Gray
                        }
                    )
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidDetailScreen(
    kid: Kid,
    onBackClick: () -> Unit,
    onAttendanceStatusChange: (newStatus: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(kid.name) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Face, contentDescription = "Kid's Profile Picture", modifier = Modifier.size(120.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = kid.name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val outAlpha by animateFloatAsState(targetValue = if (kid.attendanceStatus == "OUT") 1.0f else 0.5f, label = "OutButtonAlpha")
                    Button(onClick = { onAttendanceStatusChange("OUT") }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray), modifier = Modifier.alpha(outAlpha)) { Text("OUT") }
                    val sickAlpha by animateFloatAsState(targetValue = if (kid.attendanceStatus == "SICK") 1.0f else 0.5f, label = "SickButtonAlpha")
                    Button(onClick = { onAttendanceStatusChange("SICK") }, colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow), modifier = Modifier.alpha(sickAlpha)) { Text("SICK", color = Color.Black) }
                    val inAlpha by animateFloatAsState(targetValue = if (kid.attendanceStatus == "IN") 1.0f else 0.5f, label = "InButtonAlpha")
                    Button(onClick = { onAttendanceStatusChange("IN") }, colors = ButtonDefaults.buttonColors(containerColor = Color.Green), modifier = Modifier.alpha(inAlpha)) { Text("IN CARE") }
                }
            }
            item { SectionTitle("Allergies") }
            item { Text(kid.allergies.joinToString(", ").ifEmpty { "None specified" }) }
            item { SectionTitle("Need to Know") }
            item { Text(kid.needToKnow.ifEmpty { "Nothing specific" }) }
            item { SectionTitle("Primary Guardian") }
            item { GuardianInfoView(kid.primaryGuardian) }
            kid.secondaryGuardian?.let {
                item { SectionTitle("Secondary Guardian") }
                item { GuardianInfoView(it) }
            }
            if (kid.authorizedPickups.isNotEmpty()) {
                item { SectionTitle("Authorized Pickups") }
                kid.authorizedPickups.forEach { pickup -> item { GuardianInfoView(pickup, isAuthorizedPickup = true) } }
            }
            item { SectionTitle("Address") }
            item { Text(kid.address.ifEmpty { "Not specified" }) }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun GuardianInfoView(guardian: Guardian, isAuthorizedPickup: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text("Name: ${guardian.name}", style = MaterialTheme.typography.bodyLarge)
        Text("Phone: ${guardian.phone}", style = MaterialTheme.typography.bodyMedium)
        Text("Email: ${guardian.email}", style = MaterialTheme.typography.bodyMedium)
        if (isAuthorizedPickup || guardian.relationship.isNotBlank()) {
            Text("Relationship: ${guardian.relationship}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventForm(
    onSave: (CalendarEvent) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var eventTitle by remember { mutableStateOf("") }
    var eventDescription by remember { mutableStateOf("") }
    var isAllDayEvent by remember { mutableStateOf(false) }
    var eventDate by remember { mutableStateOf("") }
    var eventStartTime by remember { mutableStateOf("") }
    var eventEndTime by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Event") },
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel") } }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = eventTitle,
                    onValueChange = { eventTitle = it },
                    label = { Text("Event Title") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("All-day event", modifier = Modifier.weight(1f))
                    Switch(checked = isAllDayEvent, onCheckedChange = { isAllDayEvent = it })
                }
            }
            item {
                Button(onClick = { /* TODO: Implement Date Picker */ eventDate = "2024-12-25" }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (eventDate.isEmpty()) "Select Date" else "Date: $eventDate")
                }
            }
            if (!isAllDayEvent) {
                item {
                    Button(onClick = { /* TODO: Implement Time Picker */ eventStartTime = "10:00" }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (eventStartTime.isEmpty()) "Select Start Time" else "Start: $eventStartTime")
                    }
                }
                item {
                    Button(onClick = { /* TODO: Implement Time Picker */ eventEndTime = "11:00" }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (eventEndTime.isEmpty()) "Select End Time" else "End: $eventEndTime")
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = eventDescription,
                    onValueChange = { eventDescription = it },
                    label = { Text("Event Description (Optional)") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onCancel) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val newEvent = CalendarEvent(
                            title = eventTitle,
                            date = eventDate.ifEmpty { "Date TBD" },
                            startTime = if (isAllDayEvent) "" else eventStartTime.ifEmpty { "Time TBD" },
                            endTime = if (isAllDayEvent) "" else eventEndTime,
                            isAllDay = isAllDayEvent,
                            description = eventDescription
                        )
                        onSave(newEvent)
                    }) { Text("Save Event") }
                }
            }
        }
    }
}

@Composable
fun EducatorCalendarScreen(modifier: Modifier = Modifier) {
    var addingEvent by remember { mutableStateOf(false) }
    val sessionAddedEvents = remember { mutableStateListOf<CalendarEvent>() }
    var currentCalendarView by remember { mutableStateOf(CalendarDisplayMode.GRID) }

    val updateEventInList = { updatedEvent: CalendarEvent, list: MutableList<CalendarEvent> ->
        val index = list.indexOfFirst { it.id == updatedEvent.id }
        if (index != -1) {
            list[index] = updatedEvent
        }
    }

    val deleteEventFromList = { eventId: String, list: MutableList<CalendarEvent> ->
        list.removeIf { it.id == eventId } // This returns Boolean
    }

    // Explicitly type handleEventUpdated to ensure it's (CalendarEvent) -> Unit
    val handleEventUpdated: (CalendarEvent) -> Unit = { updatedEvent ->
        updateEventInList(updatedEvent, sampleUpcomingEvents)
        updateEventInList(updatedEvent, samplePastEvents)
        updateEventInList(updatedEvent, sessionAddedEvents)
    }

    // Explicitly type handleEventDeleted to ensure it's (String) -> Unit
    val handleEventDeleted: (String) -> Unit = { eventId: String ->
        deleteEventFromList(eventId, sampleUpcomingEvents)
        deleteEventFromList(eventId, samplePastEvents)
        deleteEventFromList(eventId, sessionAddedEvents)
    }

    val upcomingEventsToShow = (
        sampleUpcomingEvents.toList() +
            sessionAddedEvents.filter { !it.isPast && !sampleUpcomingEvents.any { se -> se.id == it.id } }
    ).distinctBy { it.id }

    val pastEventsToShow = (
        samplePastEvents.toList() +
            sessionAddedEvents.filter { it.isPast && !samplePastEvents.any { se -> se.id == it.id } }
    ).distinctBy { it.id }

    when {
        addingEvent -> AddEventForm(
            onSave = { event ->
                sessionAddedEvents.add(event)
                addingEvent = false
            },
            onCancel = { addingEvent = false },
            modifier = modifier
        )
        currentCalendarView == CalendarDisplayMode.UPCOMING_LIST -> EventsListScreen(
            title = "Upcoming Events",
            events = upcomingEventsToShow,
            onNavigateBackToGrid = { currentCalendarView = CalendarDisplayMode.GRID },
            onDeleteEvent = handleEventDeleted, // Now (String) -> Unit
            onEventUpdated = handleEventUpdated,
            modifier = modifier
        )
        currentCalendarView == CalendarDisplayMode.PAST_LIST -> EventsListScreen(
            title = "Past Events",
            events = pastEventsToShow,
            onNavigateBackToGrid = { currentCalendarView = CalendarDisplayMode.GRID },
            onDeleteEvent = handleEventDeleted, // Now (String) -> Unit
            onEventUpdated = handleEventUpdated,
            modifier = modifier
        )
        else -> { // GRID view
            Column(modifier = modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* TODO: Previous Month */ }, enabled = false) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous Month") }
                    Text("Month Year", style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = { /* TODO: Next Month */ }, enabled = false) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Next Month") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                        Text(text = day, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth().weight(1f).background(Color.LightGray.copy(alpha = 0.2f)),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    gridItems((1..31).toList()) { day ->
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp).aspectRatio(1f)) {
                            Text(text = day.toString(), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { currentCalendarView = CalendarDisplayMode.UPCOMING_LIST }) { Text("Upcoming Events") }
                    Button(onClick = { currentCalendarView = CalendarDisplayMode.PAST_LIST }) { Text("Past Events") }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { addingEvent = true }, modifier = Modifier.fillMaxWidth()) { Text("Add New Event") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsListScreen(
    title: String,
    events: List<CalendarEvent>,
    onNavigateBackToGrid: () -> Unit,
    onDeleteEvent: (String) -> Unit, // Expects (String) -> Unit
    onEventUpdated: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onNavigateBackToGrid) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to Calendar Grid") } }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No events to display.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EventAccordionItem(
                        event = event,
                        onDeleteRequest = { onDeleteEvent(event.id) },
                        onEventUpdated = onEventUpdated
                    )
                }
            }
        }
    }
}

@Composable
fun EventAccordionItem(
    event: CalendarEvent,
    onDeleteRequest: () -> Unit,
    onEventUpdated: (CalendarEvent) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isInEditMode by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Keyed by event.id to reset if the event itself is different.
    // Initialized with event's current properties.
    var editedTitle by remember(event.id) { mutableStateOf(event.title) }
    var editedDescription by remember(event.id) { mutableStateOf(event.description) }
    var editedImageUris by remember(event.id) { mutableStateOf(event.imageUris.toMutableList()) }

    // When entering edit mode, ensure the edit states are explicitly re-initialized
    // from the event prop, in case the event prop was updated by a parent.
    if (isInEditMode) {
        // This if block is a bit of a workaround. A LaunchedEffect might be cleaner
        // but was causing issues. This ensures that if isInEditMode becomes true,
        // we re-capture from `event` if `editedTitle` somehow still holds old values.
        // The more direct initialization is now in the Edit button's onClick.
    }


    val cardModifier = if (isInEditMode) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }
    }

    Card(
        modifier = cardModifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isInEditMode) {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = {
                        val updatedEvent = event.copy(
                            title = editedTitle,
                            description = editedDescription,
                            imageUris = editedImageUris.toList()
                            // TODO: Copy other edited fields (date, time, etc.) from their respective 'edited...' states
                        )
                        onEventUpdated(updatedEvent)
                        isInEditMode = false
                        // isExpanded = true // Optional: Keep it expanded after save
                    }) { Icon(Icons.Filled.Done, contentDescription = "Save Event") }
                    IconButton(onClick = {
                        isInEditMode = false
                        // Values will revert to original `event` prop on next view,
                        // or be re-initialized from `event` if edit is clicked again.
                    }) { Icon(Icons.Filled.Close, contentDescription = "Cancel Edit") }

                } else { // View mode for header
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f).clickable { if (!isInEditMode) isExpanded = !isExpanded }
                    )
                    IconButton(onClick = {
                        // Explicitly initialize/reset edit states from the current event prop when entering edit mode
                        editedTitle = event.title
                        editedDescription = event.description
                        editedImageUris = event.imageUris.toMutableList() // Create a fresh mutable list copy

                        isInEditMode = true
                        isExpanded = true // Ensure it's expanded when editing starts
                    }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Event")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Event")
                    }
                }
            }

            if (!isInEditMode) {
                Text(
                    text = event.date + if (event.startTime.isNotEmpty() && !event.isAllDay) " at ${event.startTime}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { if (!isInEditMode) isExpanded = !isExpanded }
                )
            }

            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                if (isInEditMode) {
                    // TODO: Add editable fields for date, time, isAllDay using OutlinedTextField or custom pickers
                    OutlinedTextField(
                        value = editedDescription,
                        onValueChange = { editedDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Images:", style = MaterialTheme.typography.labelSmall)
                    LazyRow(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(editedImageUris.toList()) { imageUrl -> // Use a copy for iteration if modifying
                            Box(contentAlignment = Alignment.TopEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Face, contentDescription = imageUrl, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                                IconButton(
                                    onClick = { editedImageUris.remove(imageUrl) },
                                    modifier = Modifier.size(24.dp).padding(0.dp)
                                        .background(Color.Black.copy(alpha=0.5f), shape = MaterialTheme.shapes.small)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete Image", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        item {
                            IconButton(onClick = { /* TODO: Implement add image functionality */ }) {
                                Icon(Icons.Filled.Add, contentDescription = "Add Photo", modifier = Modifier.size(100.dp))
                            }
                        }
                    }

                } else { // View mode for expanded content
                    Text(event.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                    if (event.imageUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Images:", style = MaterialTheme.typography.labelSmall)
                        LazyRow(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(event.imageUris) { imageName ->
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Face, contentDescription = imageName, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                }
            }
        } // end Column
    } // end Card

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete event?") },
            text = { Text("Are you sure you want to delete this event? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteRequest()
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}



@Composable
fun PlaceholderScreen(section: EducatorSection, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Welcome to ${section.title}", style = MaterialTheme.typography.headlineMedium)
    }
}

// Previews
@Preview(showBackground = true, name = "Role Selection Screen")
@Composable
fun RoleSelectionScreenPreview() { KiddozzTheme { RoleSelectionScreen({}, {}) } }

@Preview(showBackground = true, name = "Educator Dashboard - Kids Overview")
@Composable
fun EducatorDashboardKidsPreview() { KiddozzTheme { EducatorDashboardScreen({}, {}, sampleKidsState) } }

@Preview(showBackground = true, name = "Kid Detail Screen Preview")
@Composable
fun KidDetailScreenPreview() { KiddozzTheme { KidDetailScreen(sampleKidsState.first(), {}, {}) } }

@Preview(showBackground = true, name = "Kiddoz Card Preview")
@Composable
fun KiddozCardPreview() { KiddozzTheme { KiddozCard(kid = sampleKidsState.first(), onClick = {}) } }

@Preview(showBackground = true, name = "Educator Calendar - Grid View")
@Composable
fun EducatorCalendarScreenGridPreview() { KiddozzTheme { EducatorCalendarScreen() } }

@Preview(showBackground = true, name = "Add Event Form Preview")
@Composable
fun AddEventFormPreview() { KiddozzTheme { AddEventForm({}, {}) } }

@Preview(showBackground = true, name = "Events List Screen - Upcoming")
@Composable
fun EventsListScreenUpcomingPreview() {
    KiddozzTheme {
        EventsListScreen("Upcoming Events", sampleUpcomingEvents, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Event Accordion Item - Collapsed")
@Composable
fun EventAccordionItemCollapsedPreview() {
    KiddozzTheme { EventAccordionItem(sampleUpcomingEvents.first(), {}, {}) }
}

@Preview(showBackground = true, name = "Event Accordion Item - Expanded View")
@Composable
fun EventAccordionItemExpandedViewPreview() {
    KiddozzTheme {
        val event = sampleUpcomingEvents.first().copy(imageUris = listOf("img1.png", "img2.png"))
        EventAccordionItem(event = event, {}, {})
    }
}

@Preview(showBackground = true, name = "Event Accordion Item - Edit Mode")
@Composable
fun EventAccordionItemEditModePreview() {
    KiddozzTheme {
        var eventToEdit by remember { mutableStateOf(sampleUpcomingEvents.first().copy(imageUris = listOf("img1.png", "img2.png"))) }
        EventAccordionItem(
            event = eventToEdit,
            onDeleteRequest = {},
            onEventUpdated = { updatedEvent -> eventToEdit = updatedEvent }
        )
    }
}

