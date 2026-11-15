package org.fossify.calendar.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.Period

class DayLabelsTest {
    private val start = LocalDate.of(2025, 1, 15)

    @Test
    fun elapsedIsZeroOnTheStartDay() {
        assertEquals(Period.ZERO, DayLabels.elapsed(start, start))
    }

    @Test
    fun elapsedIsNullBeforeTheStartDay() {
        assertNull(DayLabels.elapsed(start, start.minusDays(1)))
    }

    @Test
    fun elapsedCountsYearsMonthsAndDays() {
        assertEquals(Period.of(1, 9, 30), DayLabels.elapsed(start, LocalDate.of(2026, 11, 14)))
        assertEquals(Period.of(1, 10, 0), DayLabels.elapsed(start, LocalDate.of(2026, 11, 15)))
        assertEquals(Period.of(0, 11, 30), DayLabels.elapsed(start, LocalDate.of(2026, 1, 14)))
        assertEquals(Period.of(2, 0, 0), DayLabels.elapsed(start, LocalDate.of(2027, 1, 15)))
    }

    @Test
    fun elapsedHandlesMonthEnds() {
        val endOfMonth = LocalDate.of(2025, 1, 31)
        assertEquals(Period.of(0, 0, 28), DayLabels.elapsed(endOfMonth, LocalDate.of(2025, 2, 28)))
        // Jan 31 + 1 month is clamped to Feb 28, then one more day
        assertEquals(Period.of(0, 1, 1), DayLabels.elapsed(endOfMonth, LocalDate.of(2025, 3, 1)))
        assertEquals(Period.of(0, 2, 0), DayLabels.elapsed(endOfMonth, LocalDate.of(2025, 3, 31)))
    }

    @Test
    fun elapsedHandlesLeapDays() {
        val leapDay = LocalDate.of(2024, 2, 29)
        // Feb 29 + 11 months is Jan 29, then 30 days to Feb 28
        assertEquals(Period.of(0, 11, 30), DayLabels.elapsed(leapDay, LocalDate.of(2025, 2, 28)))
        assertEquals(Period.of(1, 0, 1), DayLabels.elapsed(leapDay, LocalDate.of(2025, 3, 1)))
        assertEquals(Period.of(4, 0, 0), DayLabels.elapsed(leapDay, LocalDate.of(2028, 2, 29)))
    }

    @Test
    fun parseDayCodeReadsValidCodes() {
        assertEquals(start, DayLabels.parseDayCode("20250115"))
        assertEquals(LocalDate.of(2024, 2, 29), DayLabels.parseDayCode("20240229"))
    }

    @Test
    fun parseDayCodeRejectsInvalidCodes() {
        assertNull(DayLabels.parseDayCode(""))
        assertNull(DayLabels.parseDayCode("2025-01-15"))
        assertNull(DayLabels.parseDayCode("20250230"))
        assertNull(DayLabels.parseDayCode("20250229"))
        assertNull(DayLabels.parseDayCode("20251301"))
        assertNull(DayLabels.parseDayCode("abc"))
        assertNull(DayLabels.parseDayCode("20250115+0100"))
        assertNull(DayLabels.parseDayCode("202501150"))
    }

    @Test
    fun dayCodeRoundTrips() {
        assertEquals("20250115", DayLabels.toDayCode(start))
        assertEquals(start, DayLabels.parseDayCode(DayLabels.toDayCode(start)))
    }
}
