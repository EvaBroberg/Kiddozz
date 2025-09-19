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
            val classes = listOf("Class A", "Class B", "Class C")
            Kid(
                id = "kid_${index + 1}",
                name = "Kid ${index + 1}",
                age = 3 + (index % 5), // Ages 3-7
                className = classes[index % classes.size] // Distribute across classes
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
        imageUris = listOf(
            "https://picsum.photos/200/150?random=1",
            "android.resource://fi.kidozz.app/drawable/placeholder_image"
        ),
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
        imageUris = listOf("android.resource://fi.kidozz.app/drawable/placeholder_image"),
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
        imageUris = listOf("android.resource://fi.kidozz.app/drawable/placeholder_image"),
        isPast = true
    ),
    CalendarEvent(
        title = "Sports Day",
        date = "2024-02-15",
        startTime = "11:00",
        dateTime = parseDateTime("2024-02-15", "11:00"),
        description = "A wonderful day of sports and teamwork.",
        imageUris = listOf(
            "android.resource://fi.kidozz.app/drawable/placeholder_image",
            "android.resource://fi.kidozz.app/drawable/placeholder_image"
        ),
        isPast = true
    )
)
