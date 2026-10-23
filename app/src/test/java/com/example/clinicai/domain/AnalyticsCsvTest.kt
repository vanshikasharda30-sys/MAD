package com.example.clinicai.domain

import com.example.clinicai.data.model.DoctorStat
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsCsvTest {
    @Test fun headerAndRows() {
        val csv = AnalyticsCsv.build(
            listOf(
                DoctorStat("1", "Dr. A", appointmentCount = 5, noShowCount = 1, avgRating = 4.5, reviewCount = 2),
                DoctorStat("2", "Dr. B", appointmentCount = 0, noShowCount = 0, avgRating = 0.0, reviewCount = 0),
            ),
        )
        assertEquals("Doctor,Appointments,No-shows,Avg Rating,Reviews\nDr. A,5,1,4.5,2\nDr. B,0,0,,0", csv)
    }

    @Test fun quotesFieldsContainingCommasOrQuotes() {
        assertEquals("\"Smith, John\"", AnalyticsCsv.escape("Smith, John"))
        assertEquals("\"say \"\"hi\"\"\"", AnalyticsCsv.escape("say \"hi\""))
        assertEquals("plain", AnalyticsCsv.escape("plain"))
    }
}
