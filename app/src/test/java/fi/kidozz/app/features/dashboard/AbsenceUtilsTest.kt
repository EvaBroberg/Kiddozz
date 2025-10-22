package fi.kidozz.app.features.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AbsenceUtilsTest {

    @Test
    fun onlyNewDates_areReturned() {
        val existing = setOf(LocalDate.parse("2025-10-21"), LocalDate.parse("2025-10-27"))
        val selected = setOf(LocalDate.parse("2025-10-21"), LocalDate.parse("2025-10-28"))
        val expected = setOf(LocalDate.parse("2025-10-28"))

        val result = computeNewAbsenceDates(selected, existing)
        assertEquals(expected, result)
    }

    @Test
    fun allNew_whenNoExisting() {
        val existing = emptySet<LocalDate>()
        val selected = setOf(LocalDate.parse("2025-10-30"))
        assertEquals(selected, computeNewAbsenceDates(selected, existing))
    }

    @Test
    fun empty_whenAllExisting() {
        val existing = setOf(LocalDate.parse("2025-10-21"))
        val selected = setOf(LocalDate.parse("2025-10-21"))
        assertEquals(emptySet<LocalDate>(), computeNewAbsenceDates(selected, existing))
    }
}
