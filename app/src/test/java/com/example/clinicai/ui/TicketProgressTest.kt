package com.example.clinicai.ui

import com.example.clinicai.data.model.CurrentlyServing
import com.example.clinicai.data.model.QueueStatus
import com.example.clinicai.data.model.WaitingEntry
import com.example.clinicai.ui.checkin.TicketProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TicketProgressTest {
    private val status = QueueStatus(
        doctorId = "d",
        currentlyServing = CurrentlyServing(queueNumber = 3),
        waiting = listOf(WaitingEntry("e4", 4, 1, 0), WaitingEntry("e5", 5, 2, 15), WaitingEntry("e6", 6, 3, 30)),
    )

    @Test fun peopleAheadIsPositionMinusOne() {
        val p = TicketProgress.from(status, 6)
        assertEquals(2, p.peopleAhead)
        assertEquals(30, p.estimatedWaitMinutes)
        assertEquals(3, p.nowServing)
        assertFalse(p.isYourTurn)
    }

    @Test fun nextInLineHasZeroAhead() {
        assertEquals(0, TicketProgress.from(status, 4).peopleAhead)
    }

    @Test fun yourTurnWhenServingYourNumber() {
        val p = TicketProgress.from(status, 3)
        assertTrue(p.isYourTurn)
        assertNull(p.peopleAhead)
    }

    @Test fun unknownTicketHasNoWaitInformation() {
        val p = TicketProgress.from(status, 99)
        assertNull(p.peopleAhead)
        assertNull(p.estimatedWaitMinutes)
    }
}
