package com.example.clinicai.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test fun acceptsRealisticEmails() {
        assertTrue(Validators.isValidEmail("jane.doe+clinic@gmail.com"))
        assertTrue(Validators.isValidEmail("  a@b.co  "))
    }

    @Test fun rejectsBadEmails() {
        listOf("", "plain", "a@b", "@b.com", "a b@c.com", "a@@b.com").forEach { assertFalse(it, Validators.isValidEmail(it)) }
    }

    @Test fun passwordNeedsEightCharacters() {
        assertFalse(Validators.isValidPassword("1234567"))
        assertTrue(Validators.isValidPassword("12345678"))
    }

    @Test fun verificationCodeIsExactlySixDigits() {
        assertTrue(Validators.isValidVerificationCode("123456"))
        assertTrue(Validators.isValidVerificationCode(" 123456 "))
        assertFalse(Validators.isValidVerificationCode("12345"))
        assertFalse(Validators.isValidVerificationCode("1234567"))
        assertFalse(Validators.isValidVerificationCode("12a456"))
    }

    @Test fun phoneNeedsSevenToFifteenDigits() {
        assertTrue(Validators.isValidPhone("+91 98765 43210"))
        assertFalse(Validators.isValidPhone("12345"))
        assertFalse(Validators.isValidPhone("1".repeat(16)))
    }

    @Test fun numbers() {
        assertTrue(Validators.isPositiveInt("15"))
        assertFalse(Validators.isPositiveInt("0"))
        assertFalse(Validators.isPositiveInt("abc"))
        assertTrue(Validators.isNonNegativeInt("0"))
        assertFalse(Validators.isNonNegativeInt("-1"))
    }

    @Test fun ratingBetweenOneAndFive() {
        assertFalse(Validators.isValidRating(0))
        assertTrue(Validators.isValidRating(1))
        assertTrue(Validators.isValidRating(5))
        assertFalse(Validators.isValidRating(6))
    }

    @Test fun bookingCheckMirrorsBackendSchema() {
        assertEquals(Validators.BookingCheck.Ok, Validators.checkBooking("2026-10-26", "10:00", "fever"))
        assertEquals(Validators.BookingCheck.Ok, Validators.checkBooking("2026-10-26", "10:00", null))
        assertEquals(Validators.BookingCheck.MissingDate, Validators.checkBooking(null, "10:00", null))
        assertEquals(Validators.BookingCheck.MissingDate, Validators.checkBooking("26/10/2026", "10:00", null))
        assertEquals(Validators.BookingCheck.MissingSlot, Validators.checkBooking("2026-10-26", null, null))
        assertEquals(Validators.BookingCheck.MissingSlot, Validators.checkBooking("2026-10-26", "10am", null))
        assertEquals(Validators.BookingCheck.NotesTooLong, Validators.checkBooking("2026-10-26", "10:00", "x".repeat(501)))
        assertEquals(Validators.BookingCheck.Ok, Validators.checkBooking("2026-10-26", "10:00", "x".repeat(500)))
    }
}
