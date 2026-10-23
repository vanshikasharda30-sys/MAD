package com.example.clinicai.testing

import com.example.clinicai.data.model.Appointment
import com.example.clinicai.data.model.Clinic
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.data.model.Specialization

/** Small builders shared by the unit tests. Values are test data, never shipped in the app. */
object Fixtures {
    val clinic = Clinic(
        id = "clinic_t", name = "Test Clinic", address = "1 Test Road", area = "Testville", city = "Mumbai",
        pincode = "400001", phone = "+91 22 0000 0000", email = "t@example.com",
    )

    val doctor = Doctor(
        id = "doc_t", clinicId = "clinic_t", specializationId = "spec_gp", name = "Dr. Ada Test",
        qualification = "MBBS", experienceYears = 10, consultationFee = 500, languages = listOf("English"),
        slotDurationMinutes = 15, avgRating = 4.5, reviewCount = 2,
        specialization = Specialization("spec_gp", "General Physician"), clinic = clinic,
    )

    fun appointment(
        id: String = "appt_1",
        status: String = "confirmed",
        depositAmount: Int? = null,
        depositStatus: String? = "PENDING",
        doctor: Doctor? = null,
    ) = Appointment(
        id = id, patientId = "pat_1", doctorId = "doc_t", type = "SCHEDULED", status = status,
        date = "2026-10-26T00:00:00.000Z", startTime = "10:00", notes = "fever", createdAt = "2026-10-23T10:15:00.000Z",
        depositAmount = depositAmount, depositStatus = depositStatus, doctor = doctor,
    )
}
