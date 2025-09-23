package fi.kidozz.app.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.time.LocalDateTime
import java.util.UUID

@Parcelize
data class CalendarEvent(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var date: String = "", // Kept for backward compatibility
    var startTime: String = "", // Kept for backward compatibility
    var dateTime: @RawValue LocalDateTime,
    var endTime: String = "",
    var isAllDay: Boolean = false,
    var description: String = "",
    var imageUris: @RawValue List<String> = emptyList(),
    var isPast: Boolean = false
) : Parcelable
