package com.example.clinicai.domain

import com.example.clinicai.data.model.Appointment
import com.example.clinicai.data.model.Doctor

/** Who the receipt is for; comes from the signed-in account, not from user-typed receipt fields. */
data class PatientInfo(
    val name: String?,
    val email: String?,
    val phone: String?,
)

object ReceiptBuilder {

    /**
     * @param appointment the row confirmed by the backend (`POST /api/appointments` or the history API)
     * @param doctor doctor details to print when the appointment row doesn't embed them (the booking response doesn't)
     */
    fun build(appointment: Appointment, doctor: Doctor?, patient: PatientInfo?): ReceiptData {
        val resolvedDoctor = appointment.doctor ?: doctor
        val clinic = resolvedDoctor?.clinic
        val deposit = appointment.depositAmount
        val patientFromServer = appointment.patient

        return ReceiptData(
            appointmentId = appointment.id,
            status = appointment.status,
            bookedAt = appointment.createdAt,
            patientName = patientFromServer?.name?.takeIf { it.isNotBlank() } ?: patient?.name,
            patientEmail = patientFromServer?.email?.takeIf { it.isNotBlank() } ?: patient?.email,
            patientPhone = (patientFromServer?.phone ?: patient?.phone)?.takeIf { it.isNotBlank() },
            doctorName = resolvedDoctor?.name ?: "Doctor",
            specialization = resolvedDoctor?.specialization?.name,
            qualification = resolvedDoctor?.qualification?.takeIf { it.isNotBlank() },
            date = appointment.dateOnly,
            startTime = appointment.startTime,
            appointmentType = appointment.type,
            clinicName = clinic?.name,
            clinicAddress = clinic?.fullAddress?.takeIf { it.isNotBlank() },
            clinicPhone = clinic?.contactPhone?.display,
            consultationFee = resolvedDoctor?.consultationFee,
            depositAmountPaise = deposit,
            depositStatus = if (deposit == null) null else appointment.depositStatus,
            depositPaidAt = appointment.paidAt,
            paymentReference = appointment.razorpayPaymentId,
            notes = appointment.notes?.takeIf { it.isNotBlank() },
        )
    }
}
