package com.example.clinicai.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SlotGrouperTest {
    @Test fun groupsLikeTheWebsite() {
        val groups = SlotGrouper.group(listOf("10:00", "11:45", "12:00", "16:45", "17:00", "19:45"))
        assertEquals(listOf(DayPart.MORNING, DayPart.AFTERNOON, DayPart.EVENING), groups.map { it.part })
        assertEquals(listOf("10:00", "11:45"), groups[0].slots)
        assertEquals(listOf("12:00", "16:45"), groups[1].slots)
        assertEquals(listOf("17:00", "19:45"), groups[2].slots)
    }

    @Test fun omitsEmptyGroups() {
        val groups = SlotGrouper.group(listOf("17:00"))
        assertEquals(1, groups.size)
        assertEquals(DayPart.EVENING, groups[0].part)
    }

    @Test fun emptyInputGivesNoGroups() {
        assertTrue(SlotGrouper.group(emptyList()).isEmpty())
    }
}
