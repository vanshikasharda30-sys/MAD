package com.example.clinicai.data.model

data class Doctor(
    val id: String,
    val clinicId: String = "",
    val specializationId: String = "",
    val name: String,
    val qualification: String = "",
    val experienceYears: Int = 0,
    val consultationFee: Int = 0,
    val languages: List<String> = emptyList(),
    val bio: String = "",
    val slotDurationMinutes: Int = 0,
    val avgConsultationMinutes: Int = 0,
    val avgRating: Double = 0.0,
    val reviewCount: Int = 0,
    val isAvailable: Boolean = true,
    val isActive: Boolean = true,
    val currentStatus: String? = null,
    val specialization: Specialization? = null,
    val clinic: Clinic? = null,
    /** Only present on `GET /api/doctors/nearby`. */
    val distanceKm: Double? = null,
) {
    val specializationName: String get() = specialization?.name.orEmpty()
    val clinicName: String get() = clinic?.name.orEmpty()
    val hasReviews: Boolean get() = reviewCount > 0

    val initials: String
        get() = name.replace(Regex("^Dr\\.?\\s+", RegexOption.IGNORE_CASE), "")
            .split(" ")
            .mapNotNull { it.firstOrNull() }
            .take(2)
            .joinToString("")
            .uppercase()
}

data class DoctorSearchMeta(
    val page: Int = 1,
    val pageSize: Int = 20,
    val total: Int = 0,
    val totalPages: Int = 1,
)

data class AvailabilityData(
    val date: String,
    val slots: List<String>,
)
