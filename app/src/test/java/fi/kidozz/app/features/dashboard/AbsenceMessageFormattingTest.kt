package fi.kidozz.app.features.dashboard

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Unit tests for absence message formatting with range compression.
 */
class AbsenceMessageFormattingTest {

    private fun formatAbsenceMessageWithRanges(
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
                // still in a consecutive streak
                prev = current
            } else {
                // close the range
                ranges.add(
                    if (rangeStart == prev) {
                        rangeStart.format(formatter)
                    } else {
                        "from ${rangeStart.format(formatter)} to ${prev.format(formatter)}"
                    }
                )
                rangeStart = current
                prev = current
            }
        }

        // close the last range
        ranges.add(
            if (rangeStart == prev) {
                rangeStart.format(formatter)
            } else {
                "from ${rangeStart.format(formatter)} to ${prev.format(formatter)}"
            }
        )

        val joined = ranges.joinToString(", ")
        return "$kidName is $absenceType $joined"
    }

    @Test
    fun `formatAbsenceMessageWithRanges - empty list returns no absences message`() {
        val result = formatAbsenceMessageWithRanges("Emma Johnson", "on holiday", emptyList())
        assertEquals("Emma Johnson has no recorded absences.", result)
    }

    @Test
    fun `formatAbsenceMessageWithRanges - single day shows individual date`() {
        val dates = listOf(LocalDate.of(2025, 9, 10))
        val result = formatAbsenceMessageWithRanges("Emma Johnson", "on holiday", dates)
        assertEquals("Emma Johnson is on holiday Sep 10", result)
    }

    @Test
    fun `formatAbsenceMessageWithRanges - two consecutive days shows range`() {
        val dates = listOf(
            LocalDate.of(2025, 9, 1),
            LocalDate.of(2025, 9, 2)
        )
        val result = formatAbsenceMessageWithRanges("Emma Johnson", "on holiday", dates)
        assertEquals("Emma Johnson is on holiday from Sep 01 to Sep 02", result)
    }

    @Test
    fun `formatAbsenceMessageWithRanges - three consecutive days shows range`() {
        val dates = listOf(
            LocalDate.of(2025, 9, 1),
            LocalDate.of(2025, 9, 2),
            LocalDate.of(2025, 9, 3)
        )
        val result = formatAbsenceMessageWithRanges("Emma Johnson", "on holiday", dates)
        assertEquals("Emma Johnson is on holiday from Sep 01 to Sep 03", result)
    }

    @Test
    fun `formatAbsenceMessageWithRanges - mixed consecutive and individual days`() {
        val dates = listOf(
            LocalDate.of(2025, 9, 1),
            LocalDate.of(2025, 9, 2),
            LocalDate.of(2025, 9, 3),
            LocalDate.of(2025, 9, 5),
            LocalDate.of(2025, 9, 6)
        )
        val result = formatAbsenceMessageWithRanges("Emma Johnson", "on holiday", dates)
        assertEquals("Emma Johnson is on holiday from Sep 01 to Sep 03, from Sep 05 to Sep 06", result)
    }

    @Test
    fun `formatAbsenceMessageWithRanges - all individual days shows each separately`() {
        val dates = listOf(
            LocalDate.of(2025, 9, 1),
            LocalDate.of(2025, 9, 3),
            LocalDate.of(2025, 9, 5)
        )
        val result = formatAbsenceMessageWithRanges("Emma Johnson", "on holiday", dates)
        assertEquals("Emma Johnson is on holiday Sep 01, Sep 03, Sep 05", result)
    }

    @Test
    fun `formatAbsenceMessageWithRanges - unsorted dates are handled correctly`() {
        val dates = listOf(
            LocalDate.of(2025, 9, 3),
            LocalDate.of(2025, 9, 1),
            LocalDate.of(2025, 9, 2)
        )
        val result = formatAbsenceMessageWithRanges("Emma Johnson", "on holiday", dates)
        assertEquals("Emma Johnson is on holiday from Sep 01 to Sep 03", result)
    }

    @Test
    fun `formatAbsenceMessageWithRanges - sick absence uses correct verb`() {
        val dates = listOf(
            LocalDate.of(2025, 9, 1),
            LocalDate.of(2025, 9, 2)
        )
        val result = formatAbsenceMessageWithRanges("Emma Johnson", "away", dates)
        assertEquals("Emma Johnson is away from Sep 01 to Sep 02", result)
    }

    @Test
    fun `formatAbsenceMessageWithRanges - complex mixed pattern`() {
        val dates = listOf(
            LocalDate.of(2025, 9, 1),  // Start of first range
            LocalDate.of(2025, 9, 2),  // End of first range
            LocalDate.of(2025, 9, 4),  // Individual day
            LocalDate.of(2025, 9, 7),  // Start of second range
            LocalDate.of(2025, 9, 8),  // Middle of second range
            LocalDate.of(2025, 9, 9),  // End of second range
            LocalDate.of(2025, 9, 11)  // Individual day
        )
        val result = formatAbsenceMessageWithRanges("Emma Johnson", "on holiday", dates)
        assertEquals("Emma Johnson is on holiday from Sep 01 to Sep 02, Sep 04, from Sep 07 to Sep 09, Sep 11", result)
    }

    @Test
    fun `formatAbsenceMessageWithRanges - real world example from requirements`() {
        val dates = listOf(
            LocalDate.of(2025, 9, 29),
            LocalDate.of(2025, 10, 1),
            LocalDate.of(2025, 10, 2),
            LocalDate.of(2025, 10, 3),
            LocalDate.of(2025, 10, 4)
        )
        val result = formatAbsenceMessageWithRanges("Emma Johnson", "on holiday", dates)
        assertEquals("Emma Johnson is on holiday Sep 29, from Oct 01 to Oct 04", result)
    }
}
