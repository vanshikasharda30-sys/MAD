package com.example.clinicai.domain

import com.example.clinicai.data.model.Doctor
import com.example.clinicai.data.model.Specialization

/** Client-side directory filtering, matching the website's Find a Doctor behaviour. */
object DoctorFilter {

    fun apply(
        doctors: List<Doctor>,
        query: String,
        specializationId: String?,
        distances: Map<String, Double>?,
        maxKm: Double? = null,
    ): List<Doctor> {
        val q = query.trim().lowercase()
        val filtered = doctors.filter { d ->
            val matchesSpecialty = specializationId == null || d.specializationId == specializationId
            val matchesQuery = q.isEmpty() ||
                d.name.lowercase().contains(q) ||
                d.specializationName.lowercase().contains(q) ||
                d.clinicName.lowercase().contains(q) ||
                d.clinic?.area.orEmpty().lowercase().contains(q)
            matchesSpecialty && matchesQuery
        }
        if (distances == null) return filtered
        // Distance mode: only doctors whose clinic has a real position (and, with [maxKm], is close enough), nearest first.
        return filtered
            .filter { d -> distances[d.id]?.let { maxKm == null || it <= maxKm } == true }
            .sortedBy { distances.getValue(it.id) }
    }

    fun specialties(doctors: List<Doctor>): List<Specialization> =
        doctors.mapNotNull { d ->
            val name = d.specialization?.name ?: return@mapNotNull null
            Specialization(id = d.specializationId, name = name)
        }.distinctBy { it.id }.sortedBy { it.name }
}
