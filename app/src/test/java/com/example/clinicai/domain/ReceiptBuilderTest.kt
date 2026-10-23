package com.example.clinicai.domain

import com.example.clinicai.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptBuilderTest {
    private val patient = PatientInfo("Jane Doe", "jane@example.com", "")

    @Test fun mapsEverythingFromTheConfirmedAppointment() {
        val r = ReceiptBuilder.build(Fixtures.appointment(), Fixtures.doctor, patient)
        assertEquals("appt_1", r.appointmentId)
        assertEquals("confirmed", r.status)
        assertEquals("2026-10-26", r.date)
        assertEquals("10:00", r.startTime)
        assertEquals("Dr. Ada Test", r.doctorName)
        assertEquals("General Physician", r.specialization)
        assertEquals("Test Clinic", r.clinicName)
        assertEquals("1 Test Road, Testville, Mumbai, 400001", r.clinicAddress)
        assertEquals(500, r.consultationFee)
        assertEquals("fever", r.notes)
        assertEquals("Jane Doe", r.patientName)
    }

    @Test fun placeholderClinicPhoneIsNeverPrintedOnTheBill() {
        val demo = Fixtures.doctor.copy(clinic = Fixtures.clinic.copy(phone = "+91-22-9999-0000"))
        assertNull(ReceiptBuilder.build(Fixtures.appointment(), demo, patient).clinicPhone)
        val real = Fixtures.doctor.copy(clinic = Fixtures.clinic.copy(phone = "+91-22-2445-2575"))
        assertEquals("+91 22 2445 2575", ReceiptBuilder.build(Fixtures.appointment(), real, patient).clinicPhone)
    }

    @Test fun blankPhoneIsOmittedRatherThanPrinted() {
        assertNull(ReceiptBuilder.build(Fixtures.appointment(), Fixtures.doctor, patient).patientPhone)
    }

    @Test fun noDepositMeansNoPaymentDataIsInvented() {
        val r = ReceiptBuilder.build(Fixtures.appointment(depositAmount = null), Fixtures.doctor, patient)
        assertNull(r.depositAmountPaise)
        assertNull(r.depositStatus)
        assertFalse(r.hasPaidDeposit)
    }

    @Test fun paidDepositIsRecognisedOnlyWhenTheServerSaysPaid() {
        val pending = ReceiptBuilder.build(Fixtures.appointment(depositAmount = 10000, depositStatus = "PENDING"), Fixtures.doctor, patient)
        val paid = ReceiptBuilder.build(Fixtures.appointment(depositAmount = 10000, depositStatus = "PAID"), Fixtures.doctor, patient)
        assertFalse(pending.hasPaidDeposit)
        assertTrue(paid.hasPaidDeposit)
    }

    @Test fun embeddedDoctorWinsOverFallback() {
        val embedded = Fixtures.doctor.copy(name = "Dr. Embedded")
        val r = ReceiptBuilder.build(Fixtures.appointment(doctor = embedded), Fixtures.doctor, patient)
        assertEquals("Dr. Embedded", r.doctorName)
    }
}
