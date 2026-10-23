package com.example.clinicai.data.location

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationRepositoryTest {
    private var now = 1_000_000L
    private var granted = true
    private var position: Pair<Double, Double>? = 19.05 to 72.83
    private var sourceCalls = 0

    private val repo = LocationRepository(
        hasPermission = { granted },
        source = { sourceCalls++; position },
        clock = { now },
    )

    @Test fun startsWithNothingRequested() {
        assertEquals(LocationStatus.NotRequested, repo.status.value)
    }

    @Test fun withoutPermissionItNeverTouchesTheLocationSource() = runBlocking {
        granted = false
        repo.refresh()
        assertEquals(LocationStatus.PermissionDenied, repo.status.value)
        assertEquals(0, sourceCalls)
    }

    @Test fun grantedPermissionProducesAnApproximateFix() = runBlocking {
        repo.refresh()
        val status = repo.status.value as LocationStatus.Available
        assertEquals(19.05, status.location.latitude, 0.0)
        assertEquals(72.83, status.location.longitude, 0.0)
    }

    @Test fun noFixMeansUnavailableNotAGuess() = runBlocking {
        position = null
        repo.refresh()
        assertEquals(LocationStatus.Unavailable, repo.status.value)
        assertNull(repo.freshLocation())
    }

    @Test fun aRecentFixIsReusedInsteadOfAskingTheDeviceAgain() = runBlocking {
        repo.refresh()
        now += 60_000
        repo.refresh()
        assertEquals(1, sourceCalls)
    }

    @Test fun anOldFixIsRefreshed() = runBlocking {
        repo.refresh()
        now += LocationRepository.FRESH_MILLIS + 1
        assertNull(repo.freshLocation())
        repo.refresh()
        assertEquals(2, sourceCalls)
    }

    @Test fun forceRefreshAsksAgain() = runBlocking {
        repo.refresh()
        repo.refresh(force = true)
        assertEquals(2, sourceCalls)
    }

    @Test fun permissionRevokedAfterwardsFallsBackToDenied() = runBlocking {
        repo.refresh()
        granted = false
        repo.refresh(force = true)
        assertEquals(LocationStatus.PermissionDenied, repo.status.value)
    }

    @Test fun securityExceptionFromTheProviderIsTreatedAsDenied() = runBlocking {
        val throwing = LocationRepository(hasPermission = { true }, source = { throw SecurityException("revoked") })
        throwing.refresh()
        assertEquals(LocationStatus.PermissionDenied, throwing.status.value)
    }

    @Test fun aFixThatNeverArrivesBecomesUnavailableInsteadOfSpinningForever() = runBlocking {
        val hanging = LocationRepository(hasPermission = { true }, source = { kotlinx.coroutines.delay(Long.MAX_VALUE); null })
        // Uses the real 12s timeout via a virtual-time-free check: shorten by cancelling through withTimeout in the repo.
        kotlinx.coroutines.withTimeoutOrNull(LocationRepository.FIX_TIMEOUT_MILLIS + 5_000) { hanging.refresh() }
        assertEquals(LocationStatus.Unavailable, hanging.status.value)
    }

    @Test fun clearForgetsThePosition() = runBlocking {
        repo.refresh()
        repo.clear()
        assertEquals(LocationStatus.NotRequested, repo.status.value)
        assertTrue(repo.freshLocation() == null)
    }

    @Test fun fixIsMarkedApproximateWhenOnlyCoarsePermissionWasGranted() = runBlocking {
        val coarseOnly = LocationRepository(hasPermission = { true }, hasPrecisePermission = { false }, source = { 19.05 to 72.83 })
        coarseOnly.refresh()
        assertEquals(false, (coarseOnly.status.value as LocationStatus.Available).precise)
        repo.refresh()
        assertEquals(true, (repo.status.value as LocationStatus.Available).precise)
    }

    @Test fun userDecliningThePromptIsRecorded() {
        repo.markPermissionDenied()
        assertEquals(LocationStatus.PermissionDenied, repo.status.value)
    }
}
