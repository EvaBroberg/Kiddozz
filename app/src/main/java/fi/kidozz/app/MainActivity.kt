

package fi.kidozz.app

import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import fi.kidozz.app.features.dashboard.EducatorDashboardScreen

import fi.kidozz.app.features.role.RoleSelectionScreen
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
import androidx.compose.material.icons.filled.Face
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.TextStyle
import java.util.Locale

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
    var date: String = "", // Kept for now, consider removing if fully replaced by dateTime
    var startTime: String = "", // Kept for now, consider removing if fully replaced by dateTime
    var dateTime: LocalDateTime,
    var endTime: String = "",
    var isAllDay: Boolean = false,
    var description: String = "",
    var imageUris: List<String> = emptyList(),
    var isPast: Boolean = false
)

// Helper function to parse date and time strings safely
fun parseDateTime(dateStr: String, timeStr: String?): LocalDateTime {
    val date = LocalDate.parse(dateStr) // Assumes "yyyy-MM-dd"
    val time = try {
        if (!timeStr.isNullOrEmpty()) LocalTime.parse(timeStr) else LocalTime.MIDNIGHT
    } catch (e: DateTimeParseException) {
        LocalTime.MIDNIGHT // Default to midnight if parsing fails
    }
    return LocalDateTime.of(date, time)
}

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
    CalendarEvent(
        title = "Spring Festival", 
        date = "2024-05-10", 
        startTime = "10:00", 
        dateTime = parseDateTime("2024-05-10", "10:00"),
        description = "Join us for a fun-filled day of games, food, and music to celebrate spring!", 
        imageUris = listOf("spring_banner.png", "kids_playing.jpg"), 
        isPast = false
    ),
    CalendarEvent(
        title = "Parent-Teacher Meeting", 
        date = "2024-05-15", 
        startTime = "14:30", 
        dateTime = parseDateTime("2024-05-15", "14:30"),
        description = "Discuss your child's progress with their teachers. Sign up for a slot!", 
        imageUris = emptyList(), 
        isPast = false
    ),
    CalendarEvent(
        title = "Art Workshop", 
        date = "2024-05-22", 
        startTime = "09:00", 
        dateTime = parseDateTime("2024-05-22", "09:00"),
        description = "Creative workshop for all age groups. Materials provided.", 
        imageUris = listOf("art_supplies.png"), 
        isPast = false
    )
)

val samplePastEvents = mutableStateListOf(
    CalendarEvent(
        title = "Book Fair", 
        date = "2024-03-01", 
        startTime = "00:00", // Assuming all day or start time not specified
        dateTime = parseDateTime("2024-03-01", "00:00"),
        description = "Successful book fair. Thank you all for participating!", 
        imageUris = listOf("book_fair_photo1.jpg"), 
        isPast = true
    ),
    CalendarEvent(
        title = "Sports Day", 
        date = "2024-02-15", 
        startTime = "11:00", 
        dateTime = parseDateTime("2024-02-15", "11:00"),
        description = "A wonderful day of sports and teamwork.", 
        imageUris = listOf("sports_day_group.png", "medal_ceremony.jpg"), 
        isPast = true
    )
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
                // This case should ideally not happen if navigation is managed correctly
                // For robustness, navigate back or to a default screen
                currentScreen = Screen.EDUCATOR_DASHBOARD
            }
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



@Composable
fun PlaceholderScreen(section: EducatorSection, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Welcome to ${section.title}", style = MaterialTheme.typography.headlineMedium)
    }
}


@Preview(showBackground = true, name = "Kid Detail Screen Preview")
@Composable
fun KidDetailScreenPreview() { KiddozzTheme { KidDetailScreen(sampleKidsState.first(), {}, {}) } }



