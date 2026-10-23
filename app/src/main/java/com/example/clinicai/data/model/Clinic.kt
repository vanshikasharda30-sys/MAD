package com.example.clinicai.data.model

import com.example.clinicai.domain.ContactValidator
import com.example.clinicai.domain.GeoDistance

data class Clinic(
    val id: String,
    val name: String,
    val address: String = "",
    val area: String = "",
    val city: String = "",
    val pincode: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val phone: String = "",
    val email: String = "",
    val qrToken: String? = null,
    val isActive: Boolean = true,
    /** Present on `GET /api/clinics/qr/{token}`. */
    val doctors: List<Doctor>? = null,
) {
    val fullAddress: String
        get() = listOf(address, area, city, pincode)
            .filter { it.isNotBlank() }
            .joinToString(", ")

    /** Phone number safe to display/dial, or null when missing or a placeholder. */
    val contactPhone: ContactValidator.Phone? get() = ContactValidator.phone(phone)

    /** E-mail safe to display/use, or null when missing, malformed or a reserved placeholder domain. */
    val contactEmail: String? get() = ContactValidator.email(email)

    /** True when [latitude]/[longitude] are real coordinates rather than unfilled defaults. */
    val hasCoordinates: Boolean get() = GeoDistance.isValid(latitude, longitude)

    val locality: String
        get() = listOf(area, city).filter { it.isNotBlank() }.joinToString(", ")
}
