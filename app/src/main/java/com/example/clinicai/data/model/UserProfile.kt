package com.example.clinicai.data.model

/** `GET /api/me`. `type` is `PATIENT` or `STAFF`; `clinicId`/`role` are staff-only. */
data class UserProfile(
    val type: String,
    val id: String,
    val clerkUserId: String? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val clinicId: String? = null,
    val role: String? = null,
) {
    val isStaff: Boolean get() = type == "STAFF"
    val isPatient: Boolean get() = type == "PATIENT"
    val isAdmin: Boolean get() = isStaff && role == "ADMIN"
}
