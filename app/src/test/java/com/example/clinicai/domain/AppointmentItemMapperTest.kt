package com.example.clinicai.domain

import com.example.clinicai.data.model.AppointmentStatus
import com.example.clinicai.data.model.Review
import com.example.clinicai.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentItemMapperTest {
    private val withDoctor = Fixtures.appointment(doctor = Fixtures.doctor)

    @Test fun confirmedScheduledCanBeCancelled() {
        val item = AppointmentItemMapper.fromAppointment(withDoctor)
        assertTrue(item.canCancel)
        assertFalse(item.canReview)
        assertEquals("Mon, 26 Oct 2026 · 10:00 AM", item.dateText)
        assertEquals("General Physician · Test Clinic", item.subtitle)
        assertFalse(item.fromLocalCache)
    }

    @Test fun completedWithoutReviewOffersReview() {
        val item = AppointmentItemMapper.fromAppointment(withDoctor.copy(status = "completed"))
        assertTrue(item.canReview)
        assertFalse(item.canCancel)
    }

    @Test fun completedWithReviewShowsRatingInstead() {
        val item = AppointmentItemMapper.fromAppointment(withDoctor.copy(status = "completed", review = Review(rating = 4)))
        assertFalse(item.canReview)
        assertEquals(4, item.rating)
    }

    @Test fun cancelledCannotBeCancelledAgain() {
        val item = AppointmentItemMapper.fromAppointment(withDoctor.copy(status = "cancelled"))
        assertFalse(item.canCancel)
        assertEquals(AppointmentStatus.CANCELLED, item.status)
    }

    @Test fun walkInsCannotBeCancelledFromTheApp() {
        assertFalse(AppointmentItemMapper.fromAppointment(withDoctor.copy(type = "WALK_IN")).canCancel)
    }

    @Test fun cachedReceiptRowsAreReadOnly() {
        val receipt = ReceiptBuilder.build(withDoctor, null, null)
        val item = AppointmentItemMapper.fromReceipt(receipt)
        assertTrue(item.fromLocalCache)
        assertFalse(item.canCancel)
    }
}
