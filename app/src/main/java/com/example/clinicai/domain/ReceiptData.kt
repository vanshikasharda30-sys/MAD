package com.example.clinicai.domain

/**
 * Everything printed on the booking confirmation. Built only from a backend-confirmed appointment;
 * nothing here is invented — absent values stay null and the layout omits them.
 */
data class ReceiptData(
    val appointmentId: String,
    val status: String,
    /** ISO timestamp of when the booking was created on the server. */
    val bookedAt: String?,
    val patientName: String?,
    val patientEmail: String?,
    val patientPhone: String?,
    val doctorName: String,
    val specialization: String?,
    val qualification: String?,
    /** `yyyy-MM-dd`. */
    val date: String,
    /** `HH:mm`, null for walk-ins. */
    val startTime: String?,
    val appointmentType: String,
    val clinicName: String?,
    val clinicAddress: String?,
    val clinicPhone: String?,
    /** Rupees, from the doctor's listed consultation fee. */
    val consultationFee: Int?,
    /** Paise, from the Razorpay order the backend created. Null when no deposit was ever requested. */
    val depositAmountPaise: Int?,
    val depositStatus: String?,
    val depositPaidAt: String?,
    val paymentReference: String?,
    val notes: String?,
) {
    /** True only when the backend recorded a captured deposit. */
    val hasPaidDeposit: Boolean get() = depositStatus == "PAID" && depositAmountPaise != null

    val isWalkIn: Boolean get() = appointmentType == "WALK_IN"
}
