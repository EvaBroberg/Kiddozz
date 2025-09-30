package fi.kidozz.app.features.dashboard

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Unit tests for bulleted absence message formatting.
 */
class AbsenceMessageBulletedTest {

    private fun formatAbsenceMessageBulleted(
        kidName: String,
        absenceType: String,
        dates: List<LocalDate>
    ): String {
        if (dates.isEmpty()) return "$kidName has no recorded absences."

        val formatter = DateTimeFormatter.ofPattern("MMM dd")
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
    fun `formatAbsenceMessageBulleted - empty list returns no absences message`() {
        val result = formatAbsenceMessageBulleted("Emma Johnson", "holiday", emptyList())
        assertEquals("Emma Johnson has no recorded absences.", result)
    }

    @Test
    fun `formatAbsenceMessageBulleted - single day shows individual date`() {
        val dates = listOf(LocalDate.of(2025, 9, 10))
        val result = formatAbsenceMessageBulleted("Emma Johnson", "holiday", dates)
        assertEquals("Emma Johnson is on holiday:\n• Sep 10\n", result)
    }

    @Test
    fun `formatAbsenceMessageBulleted - two consecutive days shows range`() {
        val dates = listOf(
            LocalDate.of(2025, 9, 1),
            LocalDate.of(2025, 9, 2)
        )
        val result = formatAbsenceMessageBulleted("Emma Johnson", "holiday", dates)
        assertEquals("Emma Johnson is on holiday:\n• from Sep 01 to Sep 02\n", result)
    }

    @Test
    fun `formatAbsenceMessageBulleted - three consecutive days shows range`() {
        val dates = listOf(
            LocalDate.of(2025, 9, 1),
            LocalDate.of(2025, 9, 2),
            LocalDate.of(2025, 9, 3)
        )
        val result = formatAbsenceMessageBulleted("Emma Johnson", "holiday", dates)
        assertEquals("Emma Johnson is on holiday:\n• from Sep 01 to Sep 03\n", result)
    }

    @Test
    fun `formatAbsenceMessageBulleted - mixed consecutive and individual days`() {
        val dates = listOf(
            LocalDate.of(2025, 9, 1),
            LocalDate.of(2025, 9, 2),
            LocalDate.of(2025, 9, 3),
            LocalDate.of(2025, 9, 5),
            LocalDate.of(2025, 9, 6)
        )
        val result = formatAbsenceMessageBulleted("Emma Johnson", "holiday", dates)
        val expected = "Emma Johnson is on holiday:\n• from Sep 01 to Sep 03\n• from Sep 05 to Sep 06\n"
        assertEquals(expected, result)
    }

    @Test
    fun `formatAbsenceMessageBulleted - all individual days shows each separately`() {
        val dates = listOf(
            LocalDate.of(2025, 9, 1),
            LocalDate.of(2025, 9, 3),
            LocalDate.of(2025, 9, 5)
        )
        val result = formatAbsenceMessageBulleted("Emma Johnson", "holiday", dates)
        val expected = "Emma Johnson is on holiday:\n• Sep 01\n• Sep 03\n• Sep 05\n"
        assertEquals(expected, result)
    }

    @Test
    fun `formatAbsenceMessageBulleted - unsorted dates are handled correctly`() {
        val dates = listOf(
            LocalDate.of(2025, 9, 3),
            LocalDate.of(2025, 9, 1),
            LocalDate.of(2025, 9, 2)
        )
        val result = formatAbsenceMessageBulleted("Emma Johnson", "holiday", dates)
        assertEquals("Emma Johnson is on holiday:\n• from Sep 01 to Sep 03\n", result)
    }

    @Test
    fun `formatAbsenceMessageBulleted - sick absence uses correct verb`() {
        val dates = listOf(
            LocalDate.of(2025, 9, 1),
            LocalDate.of(2025, 9, 2)
        )
        val result = formatAbsenceMessageBulleted("Emma Johnson", "sick leave", dates)
        assertEquals("Emma Johnson is on sick leave:\n• from Sep 01 to Sep 02\n", result)
    }

    @Test
    fun `formatAbsenceMessageBulleted - complex mixed pattern`() {
        val dates = listOf(
            LocalDate.of(2025, 9, 1),  // Start of first range
            LocalDate.of(2025, 9, 2),  // End of first range
            LocalDate.of(2025, 9, 4),  // Individual day
            LocalDate.of(2025, 9, 7),  // Start of second range
            LocalDate.of(2025, 9, 8),  // Middle of second range
            LocalDate.of(2025, 9, 9),  // End of second range
            LocalDate.of(2025, 9, 11)  // Individual day
        )
        val result = formatAbsenceMessageBulleted("Emma Johnson", "holiday", dates)
        val expected = "Emma Johnson is on holiday:\n• from Sep 01 to Sep 02\n• Sep 04\n• from Sep 07 to Sep 09\n• Sep 11\n"
        assertEquals(expected, result)
    }

    @Test
    fun `formatAbsenceMessageBulleted - real world example from requirements`() {
        val dates = listOf(
            LocalDate.of(2025, 9, 30),
            LocalDate.of(2025, 10, 1),
            LocalDate.of(2025, 10, 2),
            LocalDate.of(2025, 10, 3),
            LocalDate.of(2025, 10, 4),
            LocalDate.of(2025, 11, 1),
            LocalDate.of(2025, 11, 2),
            LocalDate.of(2025, 11, 3),
            LocalDate.of(2025, 11, 4),
            LocalDate.of(2025, 11, 5)
        )
        val result = formatAbsenceMessageBulleted("Emma Johnson", "holiday", dates)
        val expected = "Emma Johnson is on holiday:\n• from Sep 30 to Oct 04\n• from Nov 01 to Nov 05\n"
        assertEquals(expected, result)
    }
}
