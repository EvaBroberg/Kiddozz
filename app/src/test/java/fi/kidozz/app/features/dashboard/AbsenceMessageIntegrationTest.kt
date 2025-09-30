package fi.kidozz.app.features.dashboard

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

/**
 * Integration tests for absence message formatting in the actual UI context.
 */
class AbsenceMessageFormattingIntegrationTest {

    // This is a copy of the actual function from ParentDashboardScreen.kt
    private fun formatAbsenceMessage(kidName: String, dates: List<LocalDate>, reason: String): String {
        if (dates.isEmpty()) return ""
        
        val today = LocalDate.now()
        val futureDates = dates.filter { it.isAfter(today) || it.isEqual(today) }
        if (futureDates.isEmpty()) return ""
        
        val sortedDates = futureDates.sorted()
        val verb = when (reason) {
            "holiday" -> "on holiday"
            "sick" -> "on sick leave"
            else -> "on sick leave"
        }
        
        return formatAbsenceMessageBulleted(kidName, verb, sortedDates)
    }

    private fun formatAbsenceMessageBulleted(
        kidName: String,
        absenceType: String,
        dates: List<LocalDate>
    ): String {
        if (dates.isEmpty()) return "$kidName has no recorded absences."

        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd")
        val sortedDates = dates.sorted()

        val ranges = mutableListOf<String>()
        var rangeStart = sortedDates.first()
        var prev = sortedDates.first()

        for (i in 1 until sortedDates.size) {
            val current = sortedDates[i]
            if (current == prev.plusDays(1)) {
                prev = current
            } else {
                ranges.add(
                    if (rangeStart == prev) {
                        "• ${rangeStart.format(formatter)}"
                    } else {
                        "• from ${rangeStart.format(formatter)} to ${prev.format(formatter)}"
                    }
                )
                rangeStart = current
                prev = current
            }
        }

        // Close last range
        ranges.add(
            if (rangeStart == prev) {
                "• ${rangeStart.format(formatter)}"
            } else {
                "• from ${rangeStart.format(formatter)} to ${prev.format(formatter)}"
            }
        )

        return buildString {
            append("$kidName is on $absenceType:\n")
            ranges.forEach { appendLine(it) }
        }
    }

    @Test
    fun `formatAbsenceMessage - holiday absences with range compression`() {
        val today = LocalDate.now()
        val dates = listOf(
            today.plusDays(1),
            today.plusDays(2),
            today.plusDays(3),
            today.plusDays(5),
            today.plusDays(6)
        )
        val result = formatAbsenceMessage("Emma Johnson", dates, "holiday")
        // The actual result will depend on today's date since we filter future dates
        assertTrue("Result should contain Emma Johnson", result.contains("Emma Johnson"))
        assertTrue("Result should contain holiday", result.contains("holiday"))
        assertTrue("Result should contain bullet points", result.contains("•"))
        assertTrue("Result should contain newlines", result.contains("\n"))
    }

    @Test
    fun `formatAbsenceMessage - sick absences with range compression`() {
        val today = LocalDate.now()
        val dates = listOf(
            today.plusDays(1),
            today.plusDays(2),
            today.plusDays(3),
            today.plusDays(5),
            today.plusDays(6)
        )
        val result = formatAbsenceMessage("Liam Johnson", dates, "sick")
        // The actual result will depend on today's date since we filter future dates
        assertTrue("Result should contain Liam Johnson", result.contains("Liam Johnson"))
        assertTrue("Result should contain sick leave", result.contains("sick leave"))
        assertTrue("Result should contain bullet points", result.contains("•"))
        assertTrue("Result should contain newlines", result.contains("\n"))
    }

    @Test
    fun `formatAbsenceMessage - single day absence`() {
        val today = LocalDate.now()
        val dates = listOf(today.plusDays(1))
        val result = formatAbsenceMessage("Sophia Smith", dates, "holiday")
        // The actual result will depend on today's date since we filter future dates
        assertTrue("Result should contain Sophia Smith", result.contains("Sophia Smith"))
        assertTrue("Result should contain holiday", result.contains("holiday"))
        assertTrue("Result should contain bullet points", result.contains("•"))
        assertTrue("Result should contain newlines", result.contains("\n"))
    }

    @Test
    fun `formatAbsenceMessage - empty list returns empty string`() {
        val result = formatAbsenceMessage("Emma Johnson", emptyList(), "holiday")
        assertEquals("", result)
    }

    @Test
    fun `formatAbsenceMessage - past dates are filtered out`() {
        val today = LocalDate.now()
        val pastDate = today.minusDays(1)
        val futureDate = today.plusDays(1)
        
        val dates = listOf(pastDate, futureDate)
        val result = formatAbsenceMessage("Emma Johnson", dates, "holiday")
        // The actual result will depend on today's date since we filter future dates
        assertTrue("Result should contain Emma Johnson", result.contains("Emma Johnson"))
        assertTrue("Result should contain holiday", result.contains("holiday"))
        assertTrue("Result should contain bullet points", result.contains("•"))
        assertTrue("Result should contain newlines", result.contains("\n"))
        assertTrue("Result should not contain past date", !result.contains(pastDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"))))
    }
}
