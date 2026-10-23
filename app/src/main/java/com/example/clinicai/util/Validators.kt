package com.example.clinicai.util

import com.example.clinicai.config.AppConstants

/** Client-side validation. The backend remains the authority; this only prevents obviously bad requests. */
object Validators {

    private val EMAIL = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
    private val DATE = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val TIME = Regex("^\\d{2}:\\d{2}$")

    fun isValidEmail(value: String): Boolean = EMAIL.matches(value.trim())

    fun isValidPassword(value: String): Boolean = value.length >= 8

    fun isNotBlank(value: String): Boolean = value.isNotBlank()

    /** Clerk's email verification codes are 6 digits. */
    fun isValidVerificationCode(value: String): Boolean = value.trim().matches(Regex("^\\d{6}$"))

    fun isValidPhone(value: String): Boolean {
        val digits = value.filter { it.isDigit() }
        return digits.length in 7..15
    }

    fun isPositiveInt(value: String): Boolean = value.trim().toIntOrNull()?.let { it > 0 } == true

    fun isNonNegativeInt(value: String): Boolean = value.trim().toIntOrNull()?.let { it >= 0 } == true

    sealed interface BookingCheck {
        data object Ok : BookingCheck
        data object MissingDate : BookingCheck
        data object MissingSlot : BookingCheck
        data object NotesTooLong : BookingCheck
    }

    /** Mirrors the backend's `bookAppointmentSchema` (date `YYYY-MM-DD`, time `HH:mm`, notes max 500). */
    fun checkBooking(date: String?, startTime: String?, notes: String?): BookingCheck = when {
        date == null || !DATE.matches(date) -> BookingCheck.MissingDate
        startTime == null || !TIME.matches(startTime) -> BookingCheck.MissingSlot
        (notes?.length ?: 0) > AppConstants.MAX_NOTES_LENGTH -> BookingCheck.NotesTooLong
        else -> BookingCheck.Ok
    }

    fun isValidRating(rating: Int): Boolean = rating in 1..5
}
