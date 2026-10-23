package com.example.clinicai.util

/** Masks contact details printed on shareable documents. */
object PrivacyMasker {

    /** `jane.doe@gmail.com` -> `j***@gmail.com`. */
    fun maskEmail(email: String?): String? {
        if (email.isNullOrBlank()) return null
        val at = email.indexOf('@')
        if (at <= 0) return email
        return email.first() + "***" + email.substring(at)
    }

    /** `+91 98765 43210` -> `******3210`. */
    fun maskPhone(phone: String?): String? {
        if (phone.isNullOrBlank()) return null
        val digits = phone.filter { it.isDigit() }
        if (digits.length <= 4) return digits
        return "*".repeat(digits.length - 4) + digits.takeLast(4)
    }
}
