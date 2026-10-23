package com.example.clinicai.domain

import com.example.clinicai.data.model.Appointment
import com.example.clinicai.data.model.AppointmentStatus
import com.example.clinicai.util.DateFormats

/** Row model for the appointment history list. */
data class AppointmentItem(
    val id: String,
    val doctorName: String,
    val subtitle: String,
    val dateText: String,
    val status: AppointmentStatus,
    val statusLabel: String,
    val isWalkIn: Boolean,
    val canCancel: Boolean,
    val canReview: Boolean,
    val rating: Int?,
    /** True when this row comes from a receipt cached on the device rather than from the server. */
    val fromLocalCache: Boolean,
)

object AppointmentItemMapper {

    fun fromAppointment(a: Appointment): AppointmentItem {
        val doctor = a.doctor
        val subtitle = listOfNotNull(doctor?.specialization?.name, doctor?.clinic?.name).joinToString(" · ")
        return AppointmentItem(
            id = a.id,
            doctorName = doctor?.name ?: "Doctor",
            subtitle = subtitle,
            dateText = dateText(a.dateOnly, a.startTime),
            status = a.statusEnum,
            statusLabel = ReceiptFormatter.statusText(a.status),
            isWalkIn = a.isWalkIn,
            canCancel = a.canCancel,
            canReview = a.statusEnum == AppointmentStatus.COMPLETED && a.review == null,
            rating = a.review?.rating,
            fromLocalCache = false,
        )
    }

    fun fromReceipt(r: ReceiptData): AppointmentItem = AppointmentItem(
        id = r.appointmentId,
        doctorName = r.doctorName,
        subtitle = listOfNotNull(r.specialization, r.clinicName).joinToString(" · "),
        dateText = dateText(r.date, r.startTime),
        status = AppointmentStatus.fromApi(r.status),
        statusLabel = ReceiptFormatter.statusText(r.status),
        isWalkIn = r.isWalkIn,
        canCancel = false,
        canReview = false,
        rating = null,
        fromLocalCache = true,
    )

    private fun dateText(date: String, startTime: String?): String {
        val day = DateFormats.longDate(date)
        return startTime?.let { "$day · ${DateFormats.time12h(it)}" } ?: day
    }
}
