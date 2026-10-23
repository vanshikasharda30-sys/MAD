package com.example.clinicai.data.remote

import com.example.clinicai.testing.MockApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeApiCallTest {
    private val mock = MockApi(readTimeoutMs = 300)

    @After fun tearDown() = runCatching { mock.shutdown() }.let { }

    private fun specializations() = runBlocking { safeApiCall(mock.gson) { mock.api.getSpecializations() } }

    private fun error(result: ApiResult<*>): AppError = (result as ApiResult.Failure).error

    @Test fun parsesSuccessEnvelope() {
        mock.json(200, """{"success":true,"data":[{"id":"spec_gp","name":"General Physician"}]}""")
        val result = specializations()
        assertTrue(result is ApiResult.Success)
        assertEquals("General Physician", (result as ApiResult.Success).data.single().name)
    }

    @Test fun mapsValidationErrorKeepingServerMessage() {
        mock.json(400, """{"success":false,"error":{"code":"VALIDATION_ERROR","message":"That slot is not available"}}""")
        val e = error(specializations())
        assertTrue(e is AppError.Validation)
        assertEquals("That slot is not available", e.serverMessage)
    }

    @Test fun mapsSlotConflict() {
        mock.json(409, """{"success":false,"error":{"code":"SLOT_CONFLICT","message":"That slot was just booked by someone else — please pick another"}}""")
        val e = error(specializations())
        assertTrue(e is AppError.Conflict)
        assertTrue(e.serverMessage!!.contains("just booked"))
    }

    @Test fun mapsUnauthorized() {
        mock.json(401, """{"success":false,"error":{"code":"UNAUTHORIZED","message":"Sign-in required"}}""")
        assertTrue(error(specializations()) is AppError.Unauthorized)
    }

    @Test fun mapsAccountNotProvisioned() {
        mock.json(401, """{"success":false,"error":{"code":"ACCOUNT_NOT_PROVISIONED","message":"No matching Patient"}}""")
        val e = error(specializations())
        assertTrue(e is AppError.AccountNotProvisioned)
        assertTrue(e.isAuthProblem)
    }

    @Test fun mapsForbiddenAndNotFoundJson() {
        mock.json(403, """{"success":false,"error":{"code":"FORBIDDEN","message":"nope"}}""")
        mock.json(404, """{"success":false,"error":{"code":"NOT_FOUND","message":"Appointment not found"}}""")
        assertTrue(error(specializations()) is AppError.Forbidden)
        assertTrue(error(specializations()) is AppError.NotFound)
    }

    @Test fun htmlNotFoundMeansTheRouteIsNotDeployed() {
        mock.html(404)
        assertEquals(AppError.EndpointMissing, error(specializations()))
    }

    @Test fun methodNotAllowedMeansTheRouteIsNotDeployed() {
        mock.html(405)
        assertEquals(AppError.EndpointMissing, error(specializations()))
    }

    @Test fun serverErrorKeepsStatusCode() {
        mock.html(503, "<html>down</html>")
        val e = error(specializations())
        assertTrue(e is AppError.Server)
        assertEquals(503, (e as AppError.Server).httpCode)
    }

    @Test fun malformedJsonOnSuccessIsAnInvalidResponseNotASuccess() {
        mock.json(200, "{not json")
        assertEquals(AppError.InvalidResponse, error(specializations()))
    }

    @Test fun successFlagWithoutDataIsNotASuccess() {
        mock.json(200, """{"success":true}""")
        assertEquals(AppError.InvalidResponse, error(specializations()))
    }

    @Test fun successFalseWith200IsNotASuccess() {
        mock.json(200, """{"success":false,"error":{"code":"INTERNAL_ERROR","message":"boom"}}""")
        val e = error(specializations())
        assertTrue(e is AppError.Unknown)
        assertEquals("boom", e.serverMessage)
    }

    @Test fun slowServerIsATimeout() {
        mock.server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        assertEquals(AppError.Timeout, error(specializations()))
    }

    @Test fun refusedConnectionIsServerUnreachableNotOffline() {
        mock.shutdown()
        assertEquals(AppError.ServerUnreachable, error(specializations()))
    }

    private fun failingWith(e: Exception): AppError = runBlocking {
        (safeApiCall<String>(mock.gson) { throw e } as ApiResult.Failure).error
    }

    @Test fun unresolvableHostIsOffline() {
        assertEquals(AppError.Offline, failingWith(java.net.UnknownHostException("clinicai-555888.vercel.app")))
    }

    @Test fun tlsFailureIsASecureConnectionProblem() {
        assertEquals(AppError.SecureConnectionFailed, failingWith(javax.net.ssl.SSLHandshakeException("bad cert")))
    }

    @Test fun otherSocketFailuresAreServerUnreachable() {
        assertEquals(AppError.ServerUnreachable, failingWith(java.net.NoRouteToHostException("no route")))
        assertEquals(AppError.ServerUnreachable, failingWith(java.net.SocketException("Connection reset")))
    }
}
