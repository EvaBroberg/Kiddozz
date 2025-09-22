package fi.kidozz.app.data.sample

import androidx.compose.runtime.mutableStateListOf
import fi.kidozz.app.data.models.CalendarEvent
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.data.models.TrustedAdult
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
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

// Helper function to compute age from date of birth
fun computeAge(dob: String): Int {
    val birthDate = LocalDate.parse(dob)
    return Period.between(birthDate, LocalDate.now()).years
}

val sampleKidsState = mutableStateListOf<Kid>().apply {
    addAll(
        listOf(
            Kid(
                id = "1",
                full_name = "Emma Johnson",
                dob = "2020-03-15",
                group_id = "1",
                daycare_id = "default-daycare-id",
                trusted_adults = listOf(
                    TrustedAdult(
                        name = "Sarah Johnson",
                        email = "sarah.johnson@email.com",
                        phone_num = "+1234567890",
                        address = "123 Main St, City"
                    ),
                    TrustedAdult(
                        name = "Mike Johnson",
                        email = "mike.johnson@email.com",
                        phone_num = "+1234567891",
                        address = "123 Main St, City"
                    )
                )
            ),
            Kid(
                id = "2",
                full_name = "Liam Smith",
                dob = "2019-08-22",
                group_id = "1",
                daycare_id = "default-daycare-id",
                trusted_adults = listOf(
                    TrustedAdult(
                        name = "Lisa Smith",
                        email = "lisa.smith@email.com",
                        phone_num = "+1234567892",
                        address = "456 Oak Ave, City"
                    )
                )
            ),
            Kid(
                id = "3",
                full_name = "Sophia Davis",
                dob = "2020-11-08",
                group_id = "2",
                daycare_id = "default-daycare-id",
                trusted_adults = listOf(
                    TrustedAdult(
                        name = "John Davis",
                        email = "john.davis@email.com",
                        phone_num = "+1234567893",
                        address = "789 Pine St, City"
                    ),
                    TrustedAdult(
                        name = "Maria Davis",
                        email = "maria.davis@email.com",
                        phone_num = "+1234567894",
                        address = "789 Pine St, City"
                    )
                )
            ),
            Kid(
                id = "4",
                full_name = "Noah Wilson",
                dob = "2019-05-12",
                group_id = "2",
                daycare_id = "default-daycare-id",
                trusted_adults = listOf(
                    TrustedAdult(
                        name = "Jennifer Wilson",
                        email = "jennifer.wilson@email.com",
                        phone_num = "+1234567895",
                        address = "321 Elm St, City"
                    )
                )
            ),
            Kid(
                id = "5",
                full_name = "Olivia Brown",
                dob = "2020-07-30",
                group_id = "3",
                daycare_id = "default-daycare-id",
                trusted_adults = listOf(
                    TrustedAdult(
                        name = "Robert Brown",
                        email = "robert.brown@email.com",
                        phone_num = "+1234567896",
                        address = "654 Maple Ave, City"
                    ),
                    TrustedAdult(
                        name = "Linda Brown",
                        email = "linda.brown@email.com",
                        phone_num = "+1234567897",
                        address = "654 Maple Ave, City"
                    )
                )
            ),
            Kid(
                id = "6",
                full_name = "William Taylor",
                dob = "2019-12-03",
                group_id = "3",
                daycare_id = "default-daycare-id",
                trusted_adults = listOf(
                    TrustedAdult(
                        name = "David Taylor",
                        email = "david.taylor@email.com",
                        phone_num = "+1234567898",
                        address = "987 Cedar St, City"
                    )
                )
            ),
            Kid(
                id = "7",
                full_name = "Ava Anderson",
                dob = "2021-01-18",
                group_id = "1",
                daycare_id = "default-daycare-id",
                trusted_adults = listOf(
                    TrustedAdult(
                        name = "Patricia Anderson",
                        email = "patricia.anderson@email.com",
                        phone_num = "+1234567899",
                        address = "147 Birch Rd, City"
                    ),
                    TrustedAdult(
                        name = "James Anderson",
                        email = "james.anderson@email.com",
                        phone_num = "+1234567800",
                        address = "147 Birch Rd, City"
                    )
                )
            ),
            Kid(
                id = "8",
                full_name = "James Martinez",
                dob = "2021-04-25",
                group_id = "2",
                daycare_id = "default-daycare-id",
                trusted_adults = listOf(
                    TrustedAdult(
                        name = "Carlos Martinez",
                        email = "carlos.martinez@email.com",
                        phone_num = "+1234567801",
                        address = "258 Spruce Ln, City"
                    )
                )
            ),
            Kid(
                id = "9",
                full_name = "Isabella Garcia",
                dob = "2021-08-14",
                group_id = "3",
                daycare_id = "default-daycare-id",
                trusted_adults = listOf(
                    TrustedAdult(
                        name = "Elena Garcia",
                        email = "elena.garcia@email.com",
                        phone_num = "+1234567802",
                        address = "369 Willow Way, City"
                    ),
                    TrustedAdult(
                        name = "Miguel Garcia",
                        email = "miguel.garcia@email.com",
                        phone_num = "+1234567803",
                        address = "369 Willow Way, City"
                    )
                )
            )
        )
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
