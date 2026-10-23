package com.example.clinicai.domain

import com.example.clinicai.data.model.Clinic
import com.example.clinicai.data.model.Specialization
import com.example.clinicai.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class DoctorFilterTest {
    private val cardio = Fixtures.doctor.copy(
        id = "d2", name = "Dr. Raj Heart", specializationId = "spec_cardio",
        specialization = Specialization("spec_cardio", "Cardiologist"),
        clinic = Clinic(id = "c2", name = "Hinduja Hospital", area = "Mahim"),
    )
    private val gp = Fixtures.doctor.copy(id = "d1")
    private val all = listOf(gp, cardio)

    @Test fun noFilterReturnsEverything() {
        assertEquals(2, DoctorFilter.apply(all, "", null, null).size)
    }

    @Test fun filtersBySpecialty() {
        assertEquals(listOf("d2"), DoctorFilter.apply(all, "", "spec_cardio", null).map { it.id })
    }

    @Test fun queryMatchesNameSpecialtyClinicAndArea() {
        assertEquals(listOf("d2"), DoctorFilter.apply(all, "heart", null, null).map { it.id })
        assertEquals(listOf("d2"), DoctorFilter.apply(all, "cardio", null, null).map { it.id })
        assertEquals(listOf("d2"), DoctorFilter.apply(all, "hinduja", null, null).map { it.id })
        assertEquals(listOf("d2"), DoctorFilter.apply(all, "MAHIM", null, null).map { it.id })
    }

    @Test fun combinesQueryAndSpecialty() {
        assertEquals(emptyList<String>(), DoctorFilter.apply(all, "ada", "spec_cardio", null).map { it.id })
    }

    @Test fun distanceSortPutsNearestFirstAndUnknownLast() {
        val sorted = DoctorFilter.apply(all, "", null, mapOf("d2" to 1.2))
        assertEquals(listOf("d2", "d1"), sorted.map { it.id })
    }

    @Test fun specialtiesAreDistinctAndSorted() {
        assertEquals(listOf("Cardiologist", "General Physician"), DoctorFilter.specialties(all + gp).map { it.name })
    }
}
