package com.example.clinicai.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Distance between the user and a clinic, from real coordinates only. */
object GeoDistance {

    private const val EARTH_RADIUS_KM = 6371.0088 // mean Earth radius (IUGG)

    /** What "Near me" means. Same radius the website sends to `GET /api/doctors/nearby` (`radiusKm=15`). */
    const val NEARBY_RADIUS_KM = 15.0

    /** Great-circle distance in kilometres (haversine formula). */
    fun kilometres(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return EARTH_RADIUS_KM * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /**
     * True when the pair is a plausible position. (0, 0) is rejected because it is what an
     * unfilled coordinate column deserialises to; it is in the Atlantic Ocean, not a clinic.
     */
    fun isValid(lat: Double?, lng: Double?): Boolean =
        lat != null && lng != null &&
            lat.isFinite() && lng.isFinite() &&
            lat in -90.0..90.0 && lng in -180.0..180.0 &&
            !(lat == 0.0 && lng == 0.0)

    /** Great-circle distance in metres; `kilometres * 1000.0`, kept as one place for the unit conversion. */
    fun metres(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double = kilometres(lat1, lng1, lat2, lng2) * 1000.0

    /** True when both points are valid and [km] is within [radiusKm] of each other. */
    fun isWithin(km: Double, radiusKm: Double = NEARBY_RADIUS_KM): Boolean = km.isFinite() && km <= radiusKm

    /**
     * Under 1 km in metres rounded to the nearest 10 m, otherwise kilometres:
     * `0.35` -> "350 m", `1.24` -> "1.2 km", `8.6` -> "8.6 km", `120.3` -> "120 km", `13545.2` -> "13,545 km".
     */
    fun format(km: Double): String {
        val roundedMetres = Math.round(km * 1000.0 / 10.0) * 10
        return when {
            roundedMetres < 1000 -> "%d m".format(java.util.Locale.ENGLISH, roundedMetres)
            km < 100 -> "%.1f km".format(java.util.Locale.ENGLISH, km)
            else -> "%,d km".format(java.util.Locale.ENGLISH, Math.round(km))
        }
    }
}
