package com.example.clinicai.util

import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DateFormatsTest {
    @Test fun time12hHandlesMorningNoonAndEvening() {
        assertEquals("10:00 AM", DateFormats.time12h("10:00"))
        assertEquals("12:00 PM", DateFormats.time12h("12:00"))
        assertEquals("12:15 AM", DateFormats.time12h("00:15"))
        assertEquals("5:45 PM", DateFormats.time12h("17:45"))
    }

    @Test fun time12hLeavesGarbageUntouched() {
        assertEquals("soon", DateFormats.time12h("soon"))
    }

    @Test fun appointmentDatesAreNotShiftedByTimeZone() {
        // Date-only columns arrive as UTC midnight; the calendar day must not change.
        assertEquals("Mon, 26 Oct 2026", DateFormats.longDate("2026-10-26T00:00:00.000Z"))
        assertEquals("26 Oct 2026", DateFormats.shortDate("2026-10-26"))
    }

    @Test fun invalidDateIsReturnedAsIs() {
        assertEquals("not-a-date", DateFormats.longDate("not-a-date"))
    }

    @Test fun timestampRendersInRequestedZone() {
        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        assertEquals("23 Oct 2026, 3:45 PM", DateFormats.timestamp("2026-10-23T10:15:00.000Z", ist, Locale.ENGLISH))
    }

    @Test fun timestampNullAndBlank() {
        assertNull(DateFormats.timestamp(null))
        assertNull(DateFormats.timestamp(""))
        assertNull(DateFormats.timestamp("garbage"))
    }

    @Test fun rupeesUseIndianGrouping() {
        assertEquals("500", DateFormats.rupees(500))
        assertEquals("1,200", DateFormats.rupees(1200))
        assertEquals("1,00,000", DateFormats.rupees(100000))
        assertEquals("12,34,567", DateFormats.rupees(1234567))
        assertEquals("0", DateFormats.rupees(0))
        assertEquals("999", DateFormats.rupees(999))
        assertEquals("-1,500", DateFormats.rupees(-1500))
    }

    @Test fun paiseConvertToRupees() {
        assertEquals("100", DateFormats.rupeesFromPaise(10000))
        assertEquals("50.50", DateFormats.rupeesFromPaise(5050))
        assertEquals("1,000.05", DateFormats.rupeesFromPaise(100005))
    }
}
