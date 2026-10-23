package com.example.clinicai.domain

import com.example.clinicai.data.location.LocationStatus
import com.example.clinicai.data.model.Clinic

/** What to show for "how far is this clinic from me". */
sealed interface DoctorDistance {
    /** The user hasn't asked for distances (or it's still loading): show nothing. */
    data object Hidden : DoctorDistance
    /** [approximate] is true when the user only granted approximate location, so the figure is rough. */
    data class Known(val km: Double, val approximate: Boolean = false) : DoctorDistance

    /** Asked for, but can't be computed: permission denied, no fix, or the clinic has no coordinates. */
    data object Unavailable : DoctorDistance
}

object DistanceResolver {
    fun resolve(clinic: Clinic?, status: LocationStatus): DoctorDistance = when (status) {
        LocationStatus.NotRequested, LocationStatus.Loading -> DoctorDistance.Hidden
        LocationStatus.PermissionDenied, LocationStatus.Unavailable -> DoctorDistance.Unavailable
        is LocationStatus.Available ->
            if (clinic != null && clinic.hasCoordinates) {
                DoctorDistance.Known(
                    km = GeoDistance.kilometres(status.location.latitude, status.location.longitude, clinic.latitude, clinic.longitude),
                    approximate = !status.precise,
                )
            } else {
                DoctorDistance.Unavailable
            }
    }
}
