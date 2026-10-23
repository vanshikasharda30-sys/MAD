package com.example.clinicai.data.model

enum class AppointmentStatus(val apiValue: String) {
    CONFIRMED("confirmed"),
    CANCELLED("cancelled"),
    COMPLETED("completed"),
    NO_SHOW("no_show"),
    IN_CONSULTATION("in_consultation"),
    UNKNOWN("");

    companion object {
        fun fromApi(value: String?): AppointmentStatus =
            entries.firstOrNull { it.apiValue == value } ?: UNKNOWN
    }
}

enum class DepositStatus { PENDING, PAID, FAILED, UNKNOWN;
    companion object {
        fun fromApi(value: String?): DepositStatus =
            entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

data class PatientSummary(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
)

data class Review(
    val id: String? = null,
    val rating: Int = 0,
    val reviewText: String? = null,
)

/**
 * An appointment row as returned by the ClinicAI backend. `POST /api/appointments` returns just the
 * row; `GET /api/appointments[/id]` additionally embeds the doctor (with clinic and specialization),
 * the review and, for the detail endpoint, the patient.
 */
data class Appointment(
    val id: String,
    val patientId: String? = null,
    val doctorId: String,
    val type: String = "SCHEDULED",
    val status: String = "confirmed",
    /** ISO timestamp of a date-only column, e.g. `2026-10-25T00:00:00.000Z`. */
    val date: String,
    val startTime: String? = null,
    val notes: String? = null,
    val createdAt: String? = null,
    /** Stored in paise (the Razorpay order amount). */
    val depositAmount: Int? = null,
    val depositStatus: String? = null,
    val razorpayOrderId: String? = null,
    val razorpayPaymentId: String? = null,
    val paidAt: String? = null,
    val doctor: Doctor? = null,
    val review: Review? = null,
    val patient: PatientSummary? = null,
) {
    val statusEnum: AppointmentStatus get() = AppointmentStatus.fromApi(status)
    val depositStatusEnum: DepositStatus get() = DepositStatus.fromApi(depositStatus)
    val isScheduled: Boolean get() = type == "SCHEDULED"
    val isWalkIn: Boolean get() = type == "WALK_IN"
    val canCancel: Boolean get() = statusEnum == AppointmentStatus.CONFIRMED && isScheduled
    val canPayDeposit: Boolean
        get() = canCancel && depositStatusEnum != DepositStatus.PAID
    /** `yyyy-MM-dd` part of [date]. */
    val dateOnly: String get() = date.take(10)
}

data class CancelResult(val cancelled: Boolean = false)
