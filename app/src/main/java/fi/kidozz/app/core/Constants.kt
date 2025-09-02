package fi.kidozz.app.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen {
    ROLE_SELECTION,
    EDUCATOR_DASHBOARD,
    KID_DETAIL,
    UPCOMING_EVENTS,
    PREVIOUS_EVENTS
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
