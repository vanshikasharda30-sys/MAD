package com.example.clinicai.config

object AppConstants {
    /** Chat is scoped to a clinic; the website's "Ask ClinicAI" links default to this clinic. */
    const val DEFAULT_CLINIC_ID = "clinic_01"

    const val MAX_NOTES_LENGTH = 500
    const val BOOKING_WINDOW_DAYS = 14
    const val QUEUE_POLL_INTERVAL_MS = 6_000L
    const val STAFF_QUEUE_POLL_INTERVAL_MS = 8_000L
    const val DOCTOR_PAGE_SIZE = 50

    /** Path segment the website uses for walk-in check-in links: /checkin/{qrToken}. */
    const val CHECKIN_PATH = "checkin"
    const val DOCTOR_PATH = "doctors"
}
