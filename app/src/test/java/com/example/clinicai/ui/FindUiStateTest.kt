package com.example.clinicai.ui

import com.example.clinicai.data.location.LocationStatus
import com.example.clinicai.data.location.UserLocation
import com.example.clinicai.data.model.Clinic
import com.example.clinicai.testing.Fixtures
import com.example.clinicai.ui.doctors.FindUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FindUiStateTest {
    private val far = Fixtures.doctor.copy(id = "far", clinic = Clinic(id = "c1", name = "Far", latitude = 18.9686, longitude = 72.8090))
    private val near = Fixtures.doctor.copy(id = "near", clinic = Clinic(id = "c2", name = "Near", latitude = 19.0389, longitude = 72.8410))
    private val unknown = Fixtures.doctor.copy(id = "unknown", clinic = Clinic(id = "c3", name = "No coordinates"))
    private val doctors = listOf(far, unknown, near)

    @Test fun withoutALocationNothingIsSortedOrMeasured() {
        val state = FindUiState(loading = false, doctors = doctors)
        assertTrue(state.distances.isEmpty())
        assertFalse(state.sortedByDistance)
        assertEquals(listOf("far", "unknown", "near"), state.visible.map { it.id })
    }

    @Test fun withALocationDoctorsAreSortedNearestFirstAndUnknownPositionsAreExcluded() {
        val here = LocationStatus.Available(UserLocation(19.0509, 72.8296, 0L)) // at Lilavati
        val state = FindUiState(loading = false, doctors = doctors, location = here)
        assertEquals(listOf("near", "far"), state.visible.map { it.id })
        assertEquals(1.79, state.distances.getValue("near"), 0.01)
        assertEquals(9.40, state.distances.getValue("far"), 0.01)
        assertFalse("a clinic without coordinates gets no distance", state.distances.containsKey("unknown"))
    }

    @Test fun deniedLocationLeavesTheOriginalOrder() {
        val state = FindUiState(loading = false, doctors = doctors, location = LocationStatus.PermissionDenied)
        assertEquals(listOf("far", "unknown", "near"), state.visible.map { it.id })
    }
}
