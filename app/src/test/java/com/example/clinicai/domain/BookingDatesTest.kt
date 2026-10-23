package com.example.clinicai.domain

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingDatesTest {
    private fun calendar(y: Int, m: Int, d: Int) =
        Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply { clear(); set(y, m, d, 23, 30) }

    @Test fun producesRequestedNumberOfDaysStartingToday() {
        val days = BookingDates.next(14, calendar(2026, Calendar.OCTOBER, 23))
        assertEquals(14, days.size)
        assertEquals("2026-10-23", days.first().iso)
        assertTrue(days.first().isToday)
        assertFalse(days[1].isToday)
        assertEquals("2026-11-05", days.last().iso)
    }

    @Test fun usesLocalCalendarDayEvenLateInTheEvening() {
        // 23:30 IST is still the 23rd locally; a UTC-based calculation would already be wrong near midnight.
        assertEquals("2026-10-23", BookingDates.next(1, calendar(2026, Calendar.OCTOBER, 23)).single().iso)
    }

    @Test fun weekdayLabelsAreUppercase() {
        val days = BookingDates.next(3, calendar(2026, Calendar.OCTOBER, 23))
        assertEquals(listOf("FRI", "SAT", "SUN"), days.map { it.weekday })
    }

    @Test fun crossesMonthBoundary() {
        val days = BookingDates.next(3, calendar(2026, Calendar.OCTOBER, 30))
        assertEquals(listOf("2026-10-30", "2026-10-31", "2026-11-01"), days.map { it.iso })
    }
}
