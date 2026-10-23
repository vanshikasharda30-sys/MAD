package com.example.clinicai.domain

import com.example.clinicai.data.location.LocationStatus
import com.example.clinicai.data.location.UserLocation
import com.example.clinicai.data.model.Clinic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceResolverTest {
    private val lilavati = Clinic(id = "c1", name = "Lilavati", latitude = 19.0509, longitude = 72.8296)
    private val noCoordinates = Clinic(id = "c2", name = "No coordinates") // defaults to (0.0, 0.0)
    private val atKokilaben = LocationStatus.Available(UserLocation(19.1306, 72.8259, 0L))

    @Test fun knownDistanceUsesTheClinicsRealCoordinates() {
        val result = DistanceResolver.resolve(lilavati, atKokilaben) as DoctorDistance.Known
        assertEquals(8.8708, result.km, 0.005)
    }

    @Test fun approximateAccessIsMarkedSoTheUiCanSayAbout() {
        val rough = LocationStatus.Available(UserLocation(19.1306, 72.8259, 0L), precise = false)
        val result = DistanceResolver.resolve(lilavati, rough) as DoctorDistance.Known
        assertEquals(true, result.approximate)
        assertEquals(false, (DistanceResolver.resolve(lilavati, atKokilaben) as DoctorDistance.Known).approximate)
    }

    @Test fun standingAtTheClinicIsZero() {
        val here = LocationStatus.Available(UserLocation(19.0509, 72.8296, 0L))
        assertEquals(0.0, (DistanceResolver.resolve(lilavati, here) as DoctorDistance.Known).km, 1e-6)
    }

    @Test fun missingClinicCoordinatesNeverProduceAFakeDistance() {
        assertEquals(DoctorDistance.Unavailable, DistanceResolver.resolve(noCoordinates, atKokilaben))
        assertEquals(DoctorDistance.Unavailable, DistanceResolver.resolve(null, atKokilaben))
    }

    @Test fun deniedOrFailedLocationIsUnavailable() {
        assertEquals(DoctorDistance.Unavailable, DistanceResolver.resolve(lilavati, LocationStatus.PermissionDenied))
        assertEquals(DoctorDistance.Unavailable, DistanceResolver.resolve(lilavati, LocationStatus.Unavailable))
    }

    @Test fun nothingIsShownUntilTheUserAsks() {
        assertEquals(DoctorDistance.Hidden, DistanceResolver.resolve(lilavati, LocationStatus.NotRequested))
        assertEquals(DoctorDistance.Hidden, DistanceResolver.resolve(lilavati, LocationStatus.Loading))
        assertTrue(DistanceResolver.resolve(lilavati, atKokilaben) is DoctorDistance.Known)
    }
}
