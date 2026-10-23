package com.example.clinicai.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoDistanceTest {
    // Real clinic coordinates from the production API; expected values come from an independent
    // haversine implementation (different language and code path), not from this code.
    private val lilavati = 19.0509 to 72.8296
    private val kokilaben = 19.1306 to 72.8259
    private val jaslok = 18.9686 to 72.8090
    private val hinduja = 19.0389 to 72.8410

    private fun km(a: Pair<Double, Double>, b: Pair<Double, Double>) = GeoDistance.kilometres(a.first, a.second, b.first, b.second)

    @Test fun matchesIndependentReferenceDistances() {
        assertEquals(8.8708, km(lilavati, kokilaben), 0.005)
        assertEquals(9.4041, km(lilavati, jaslok), 0.005)
        assertEquals(1.7934, km(lilavati, hinduja), 0.005)
        assertEquals(1148.096, GeoDistance.kilometres(28.6139, 77.2090, 19.0760, 72.8777), 0.5)
    }

    @Test fun samePointIsZeroAndDistanceIsSymmetric() {
        assertEquals(0.0, km(lilavati, lilavati), 1e-9)
        assertEquals(km(lilavati, jaslok), km(jaslok, lilavati), 1e-9)
    }

    @Test fun validCoordinatesAcceptRealPlacesAndRejectUnfilledDefaults() {
        assertTrue(GeoDistance.isValid(19.0509, 72.8296))
        assertFalse("(0,0) is what an unfilled column deserialises to", GeoDistance.isValid(0.0, 0.0))
        assertFalse(GeoDistance.isValid(null, 72.8))
        assertFalse(GeoDistance.isValid(91.0, 72.8))
        assertFalse(GeoDistance.isValid(19.0, 181.0))
        assertFalse(GeoDistance.isValid(Double.NaN, 72.8))
    }

    @Test fun formatsLikeTheRequirement() {
        assertEquals("2.4 km", GeoDistance.format(2.4))
        assertEquals("430 m", GeoDistance.format(0.43))
        assertEquals("350 m", GeoDistance.format(0.3503))
        assertEquals("1.0 km", GeoDistance.format(0.9996)) // rounds up to 1 km, never "1000 m"
        assertEquals("1.2 km", GeoDistance.format(1.24))
        assertEquals("8.6 km", GeoDistance.format(8.6))
        assertEquals("18.7 km", GeoDistance.format(18.74))
        assertEquals("120 km", GeoDistance.format(120.3))
        assertEquals("13,545 km", GeoDistance.format(13545.2))
    }
}
