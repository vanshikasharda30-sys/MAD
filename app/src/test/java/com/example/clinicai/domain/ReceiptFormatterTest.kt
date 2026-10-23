package com.example.clinicai.domain

import com.example.clinicai.testing.Fixtures
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptFormatterTest {
    private val ist = TimeZone.getTimeZone("Asia/Kolkata")
    private val patient = PatientInfo("Jane Doe", "jane.doe@gmail.com", "+91 98765 43210")

    private fun receipt(deposit: Int? = null, status: String = "PENDING") =
        ReceiptBuilder.build(Fixtures.appointment(depositAmount = deposit, depositStatus = status), Fixtures.doctor, patient)

    private fun ReceiptDocument.row(label: String) = sections.flatMap { it.rows }.firstOrNull { it.label == label }

    @Test fun unpaidBookingIsLabelledAsConfirmationNotPaymentReceipt() {
        val doc = ReceiptFormatter.format(receipt(), ist)
        assertEquals("Booking Confirmation", doc.title)
        assertTrue(doc.subtitle.contains("not a payment receipt"))
        assertEquals("No online payment taken", doc.row("Payment status")?.value)
        assertTrue(doc.notices.first().contains("not a payment receipt"))
    }

    @Test fun paidDepositBecomesDepositReceiptWithAmountInRupees() {
        val doc = ReceiptFormatter.format(receipt(10000, "PAID"), ist)
        assertEquals("Booking Confirmation & Deposit Receipt", doc.title)
        assertEquals("₹100", doc.row("Deposit paid")?.value)
        assertEquals("Deposit paid", doc.row("Payment status")?.value)
    }

    @Test fun pendingDepositIsNotPresentedAsPaid() {
        val doc = ReceiptFormatter.format(receipt(10000, "PENDING"), ist)
        assertNull(doc.row("Deposit paid"))
        assertEquals("Deposit not paid", doc.row("Payment status")?.value)
    }

    @Test fun showsServerBookingIdAndLocalisedTimes() {
        val doc = ReceiptFormatter.format(receipt(), ist)
        assertEquals("appt_1", doc.bookingId)
        assertEquals("appt_1", doc.row("Booking ID")?.value)
        assertEquals("23 Oct 2026, 3:45 PM", doc.row("Booked on")?.value)
        assertEquals("Mon, 26 Oct 2026 · 10:00 AM", doc.row("Date & time")?.value)
        assertEquals("CONFIRMED", doc.statusLabel)
    }

    @Test fun contactDetailsAreMasked() {
        val doc = ReceiptFormatter.format(receipt(), ist)
        val contact = doc.row("Contact")?.value.orEmpty()
        assertTrue(contact, contact.contains("j***@gmail.com"))
        assertTrue(contact, contact.contains("3210"))
        assertFalse(contact, contact.contains("jane.doe"))
        assertFalse(contact, contact.contains("98765"))
    }

    @Test fun neverInventsTaxesOrDiscounts() {
        val labels = ReceiptFormatter.format(receipt(), ist).sections.flatMap { it.rows }.map { it.label.lowercase() }
        assertFalse(labels.any { it.contains("tax") || it.contains("discount") || it.contains("total") || it.contains("gst") })
    }

    @Test fun feeIsShownAsListedConsultationFee() {
        assertEquals("₹500", ReceiptFormatter.format(receipt(), ist).row("Consultation fee (as listed)")?.value)
    }

    @Test fun fileNameIsSafe() {
        val name = ReceiptFormatter.format(receipt(), ist).fileName
        assertTrue(name, name.matches(Regex("^ClinicAI-Booking-[A-Za-z0-9]+\\.pdf$")))
        assertNotNull(name)
    }

    @Test fun cancelledStatusIsReadable() {
        assertEquals("CANCELLED", ReceiptFormatter.format(ReceiptBuilder.build(Fixtures.appointment(status = "cancelled"), Fixtures.doctor, patient), ist).statusLabel)
        assertEquals("No-show", ReceiptFormatter.statusText("no_show"))
    }
}
