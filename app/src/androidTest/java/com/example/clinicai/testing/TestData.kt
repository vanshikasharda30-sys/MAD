package com.example.clinicai.testing

import com.example.clinicai.domain.ReceiptData

/** Receipt content used by the on-device tests (test data only). */
object TestData {
    fun receipt(deposit: Int? = null, depositStatus: String? = null, notes: String? = "Fever and sore throat for two days") = ReceiptData(
        appointmentId = "cmtest0001abcdefghijklmno",
        status = "confirmed",
        bookedAt = "2026-10-23T10:15:00.000Z",
        patientName = "Jane Doe",
        patientEmail = "jane.doe@gmail.com",
        patientPhone = "+91 98765 43210",
        doctorName = "Dr. Ada Test",
        specialization = "General Physician",
        qualification = "MBBS, MD",
        date = "2026-10-26",
        startTime = "10:00",
        appointmentType = "SCHEDULED",
        clinicName = "Test Clinic and Research Centre",
        clinicAddress = "A-791, Bandra Reclamation, Bandra West, Mumbai, 400050",
        clinicPhone = "+91 22 2345 6789",
        consultationFee = 500,
        depositAmountPaise = deposit,
        depositStatus = depositStatus,
        depositPaidAt = if (depositStatus == "PAID") "2026-10-23T10:20:00.000Z" else null,
        paymentReference = if (depositStatus == "PAID") "pay_Test123456" else null,
        notes = notes,
    )
}
