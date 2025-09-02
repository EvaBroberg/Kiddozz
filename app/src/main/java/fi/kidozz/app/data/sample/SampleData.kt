package fi.kidozz.app.data.sample

import androidx.compose.runtime.mutableStateListOf
import fi.kidozz.app.data.models.CalendarEvent
import fi.kidozz.app.data.models.Guardian
import fi.kidozz.app.data.models.Kid
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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
