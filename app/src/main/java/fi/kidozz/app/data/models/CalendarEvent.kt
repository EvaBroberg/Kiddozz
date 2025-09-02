package fi.kidozz.app.data.models

import java.time.LocalDateTime
import java.util.UUID

data class CalendarEvent(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var date: String = "", // Kept for backward compatibility
    var startTime: String = "", // Kept for backward compatibility
    var dateTime: LocalDateTime,
    var endTime: String = "",
    var isAllDay: Boolean = false,
    var description: String = "",
    var imageUris: List<String> = emptyList(),
    var isPast: Boolean = false
)
