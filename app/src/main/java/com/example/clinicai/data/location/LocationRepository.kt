package com.example.clinicai.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** The user's approximate position at one moment. Held in memory only — never written to disk or logs. */
data class UserLocation(val latitude: Double, val longitude: Double, val takenAtMillis: Long)

sealed interface LocationStatus {
    /** The user hasn't asked for distances. Nothing is shown and no permission is requested. */
    data object NotRequested : LocationStatus
    data object Loading : LocationStatus
    /** [precise] is false when the user only granted "approximate" access, so distances are rough. */
    data class Available(val location: UserLocation, val precise: Boolean = true) : LocationStatus
    data object PermissionDenied : LocationStatus

    /** Permission is granted but no position could be obtained (location switched off, no fix, timeout). */
    data object Unavailable : LocationStatus
}

/** Where a position comes from. Abstracted so the logic can be tested without a device. */
fun interface LocationSource {
    suspend fun currentPosition(): Pair<Double, Double>?
}

/**
 * One-shot, approximate location for "distance from you". It asks for a single fix when the user
 * requests it (no background tracking, no continuous updates) and keeps it only in memory.
 */
class LocationRepository(
    private val hasPermission: () -> Boolean,
    private val hasPrecisePermission: () -> Boolean = hasPermission,
    private val source: LocationSource,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _status = MutableStateFlow<LocationStatus>(LocationStatus.NotRequested)
    val status: StateFlow<LocationStatus> = _status.asStateFlow()

    fun permissionGranted(): Boolean = hasPermission()

    /** A fix younger than [FRESH_MILLIS], or null. */
    fun freshLocation(): UserLocation? =
        (status.value as? LocationStatus.Available)?.location?.takeIf { clock() - it.takenAtMillis < FRESH_MILLIS }

    /** Called after the user answered the permission prompt negatively. */
    fun markPermissionDenied() {
        _status.value = LocationStatus.PermissionDenied
    }

    /** Takes one position. Re-uses a recent one instead of asking the device again. */
    suspend fun refresh(force: Boolean = false) {
        if (!hasPermission()) {
            _status.value = LocationStatus.PermissionDenied
            return
        }
        if (!force && freshLocation() != null) return
        _status.value = LocationStatus.Loading
        val position = try {
            // A fix that never arrives (GPS off, no signal) must not leave the UI spinning forever.
            withTimeoutOrNull(FIX_TIMEOUT_MILLIS) { source.currentPosition() }
        } catch (e: SecurityException) {
            _status.value = LocationStatus.PermissionDenied
            return
        }
        _status.value = if (position == null) {
            LocationStatus.Unavailable
        } else {
            LocationStatus.Available(UserLocation(position.first, position.second, clock()), precise = hasPrecisePermission())
        }
    }

    /** Forget the position (e.g. the user turned "distances" off). */
    fun clear() {
        _status.value = LocationStatus.NotRequested
    }

    companion object {
        const val FRESH_MILLIS = 5 * 60 * 1000L
        const val FIX_TIMEOUT_MILLIS = 12_000L

        fun create(context: Context): LocationRepository {
            val app = context.applicationContext
            return LocationRepository(
                hasPermission = {
                    ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                },
                hasPrecisePermission = {
                    ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                },
                source = FusedSource(app),
            )
        }
    }
}

/** Google Play services' fused provider, asked for a single balanced-power (approximate) fix. */
private class FusedSource(private val context: Context) : LocationSource {
    @SuppressLint("MissingPermission") // the caller checks permission first
    override suspend fun currentPosition(): Pair<Double, Double>? = suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cancel = CancellationTokenSource()
        cont.invokeOnCancellation { cancel.cancel() }
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancel.token)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    if (cont.isActive) cont.resume(loc.latitude to loc.longitude)
                } else {
                    // No live fix: a recent cached one is acceptable for an approximate distance.
                    client.lastLocation
                        .addOnSuccessListener { last -> if (cont.isActive) cont.resume(last?.let { it.latitude to it.longitude }) }
                        .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                }
            }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
    }
}
