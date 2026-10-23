package com.example.clinicai.domain

import com.example.clinicai.data.location.LocationStatus
import com.example.clinicai.data.location.UserLocation
import com.example.clinicai.data.model.Clinic
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.testing.Fixtures
import com.example.clinicai.ui.doctors.FindUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Near me" end to end on the pure logic: the same coordinates feed the distance shown on a card and the
 * radius filter / nearest-first sort. Coordinates are the real clinic positions served by production
 * (`GET /api/doctors/search`); expected distances were computed with an independent haversine in Node
 * (R = 6371.0088 km), not with this code. Nothing here edits real clinic data.
 */
class NearMeTest {
    private fun clinic(id: String, lat: Double, lng: Double) = Clinic(id = id, name = id, latitude = lat, longitude = lng)
    private fun doctor(id: String, clinic: Clinic) = Fixtures.doctor.copy(id = id, name = "Dr $id", clinic = clinic)

    private val lilavati = clinic("lilavati", 19.0509, 72.8296)
    private val hinduja = clinic("hinduja", 19.0389, 72.8410)       // 1.7934 km from Lilavati
    private val kokilaben = clinic("kokilaben", 19.1306, 72.8259)   // 8.8708 km
    private val jaslok = clinic("jaslok", 18.9686, 72.8090)         // 9.4041 km
    private val mulund = clinic("mulund", 19.1726, 72.9425)         // 17.9954 km: outside 15 km
    private val delhi = clinic("delhi", 28.6139, 77.2090)           // 1152.5647 km
    private val unfilled = clinic("unfilled", 0.0, 0.0)             // coordinate columns never filled in
    private val invalid = clinic("invalid", 95.0, 72.8)

    private val atLilavati = LocationStatus.Available(UserLocation(19.0509, 72.8296, 0L))

    private fun km(a: Clinic, bLat: Double, bLng: Double) = GeoDistance.kilometres(bLat, bLng, a.latitude, a.longitude)

    // 1. Two verified nearby locations
    @Test fun twoNearbyClinicsMatchTheReferenceDistance() {
        assertEquals(1.7934, km(hinduja, lilavati.latitude, lilavati.longitude), 0.005)
    }

    // 2. About 1 km apart (0.009 degrees of latitude = 1.0008 km)
    @Test fun pointsAboutOneKilometreApart() {
        assertEquals(1.0008, GeoDistance.kilometres(19.0509, 72.8296, 19.0599, 72.8296), 0.001)
        assertEquals("1.0 km", GeoDistance.format(1.0008))
    }

    // 3. Different cities
    @Test fun differentCitiesAreFarApartAndNotNearby() {
        val km = km(delhi, lilavati.latitude, lilavati.longitude)
        assertEquals(1152.5647, km, 0.5)
        assertFalse(GeoDistance.isWithin(km))
    }

    // 4. Identical user and clinic coordinates
    @Test fun standingAtTheClinicIsZeroMetres() {
        assertEquals(0.0, GeoDistance.metres(19.0509, 72.8296, 19.0509, 72.8296), 1e-6)
        assertEquals("0 m", GeoDistance.format(0.0))
        assertTrue(GeoDistance.isWithin(0.0))
    }

    // 5. Swapped latitude and longitude
    @Test fun swappedCoordinatesAreRejectedWhenOutOfRangeAndOtherwiseFarOutsideTheRadius() {
        // Western-hemisphere user (lat 37.4, lng -122.1) with the pair swapped: latitude -122.1 can't exist.
        assertFalse(GeoDistance.isValid(-122.08, 37.42))
        // Lilavati swapped is still in range, but lands ~6,841 km away, so it can never pass as "nearby".
        val swapped = GeoDistance.kilometres(19.0509, 72.8296, 72.8296, 19.0509)
        assertEquals(6841.4622, swapped, 1.0)
        assertFalse(GeoDistance.isWithin(swapped))
    }

    // 6 + 7. Missing and invalid clinic coordinates
    @Test fun clinicsWithoutRealCoordinatesGetNoDistance() {
        assertEquals(DoctorDistance.Unavailable, DistanceResolver.resolve(unfilled, atLilavati))
        assertEquals(DoctorDistance.Unavailable, DistanceResolver.resolve(invalid, atLilavati))
        assertEquals(DoctorDistance.Unavailable, DistanceResolver.resolve(null, atLilavati))
        assertFalse(GeoDistance.isValid(Double.NaN, 72.8))
        assertFalse(GeoDistance.isValid(19.0, Double.POSITIVE_INFINITY))
        assertFalse(GeoDistance.isValid(19.0, -180.5))
    }

    // 8. Missing current location: never a made-up position
    @Test fun withoutALocationNoDistanceIsInventedAndNothingIsFiltered() {
        listOf(LocationStatus.Unavailable, LocationStatus.PermissionDenied).forEach {
            assertEquals(DoctorDistance.Unavailable, DistanceResolver.resolve(lilavati, it))
            val state = FindUiState(loading = false, doctors = listOf(doctor("a", lilavati), doctor("b", delhi)), location = it)
            assertTrue(state.distances.isEmpty())
            assertEquals("all doctors stay listed, in their original order", listOf("a", "b"), state.visible.map { d -> d.id })
            assertFalse(state.nothingNearby)
        }
    }

    // 9. Metres / kilometres conversion
    @Test fun unitConversionIsExact() {
        val km = GeoDistance.kilometres(19.0509, 72.8296, 19.0389, 72.8410)
        assertEquals(km * 1000.0, GeoDistance.metres(19.0509, 72.8296, 19.0389, 72.8410), 1e-9)
        assertEquals(1.7934, 1793.4 / 1000.0, 1e-9)
        assertEquals("350 m", GeoDistance.format(0.3503))
        assertEquals("1.8 km", GeoDistance.format(1.7934))
    }

    // 10. Nearest-first sorting and radius filtering, using the same distance the card shows
    @Test fun nearMeListsOnlyClinicsWithinTheRadiusNearestFirst() {
        val doctors = listOf(
            doctor("delhi", delhi), doctor("jaslok", jaslok), doctor("mulund", mulund), doctor("unfilled", unfilled),
            doctor("lilavati", lilavati), doctor("kokilaben", kokilaben), doctor("invalid", invalid), doctor("hinduja", hinduja),
        )
        val state = FindUiState(loading = false, doctors = doctors, location = atLilavati)

        assertEquals(listOf("lilavati", "hinduja", "kokilaben", "jaslok"), state.visible.map { it.id })
        assertFalse(state.nothingNearby)

        // The figure on each card is exactly the one used to filter and sort.
        state.visible.forEach { d ->
            val shown = (DistanceResolver.resolve(d.clinic, atLilavati) as DoctorDistance.Known).km
            assertEquals(state.distances.getValue(d.id), shown, 1e-9)
            assertTrue(shown <= GeoDistance.NEARBY_RADIUS_KM)
        }
        assertEquals(17.9954, state.distances.getValue("mulund"), 0.005) // measured, then excluded by the radius
    }

    @Test fun userFarFromEveryClinicSeesAnExplicitNothingNearbyState() {
        val inDelhi = LocationStatus.Available(UserLocation(28.6139, 77.2090, 0L))
        val state = FindUiState(loading = false, doctors = listOf(doctor("a", lilavati), doctor("b", kokilaben)), location = inDelhi)
        assertTrue(state.visible.isEmpty())
        assertTrue(state.nothingNearby)
    }

    @Test fun searchAndSpecialtyStillApplyInsideTheRadius() {
        val state = FindUiState(
            loading = false,
            doctors = listOf(doctor("lilavati", lilavati), doctor("hinduja", hinduja)),
            query = "dr hinduja",
            location = atLilavati,
        )
        assertEquals(listOf("hinduja"), state.visible.map { it.id })
    }
}
