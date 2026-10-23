package com.example.clinicai.data.remote

import com.example.clinicai.testing.MockApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthInterceptorTest {
    @Test fun sendsBearerTokenWhenSignedIn() {
        val mock = MockApi(token = "jwt-abc")
        mock.json(200, """{"success":true,"data":[]}""")
        runBlocking { mock.api.getSpecializations() }
        val request = mock.server.takeRequest()
        assertEquals("Bearer jwt-abc", request.getHeader("Authorization"))
        assertEquals("application/json", request.getHeader("Accept"))
        mock.shutdown()
    }

    @Test fun sendsNoAuthorizationHeaderWhenSignedOut() {
        val mock = MockApi(token = null)
        mock.json(200, """{"success":true,"data":[]}""")
        runBlocking { mock.api.getSpecializations() }
        assertNull(mock.server.takeRequest().getHeader("Authorization"))
        mock.shutdown()
    }

    @Test fun tokenProviderFailureDoesNotBreakPublicRequests() {
        val mock = MockApi()
        // A provider that throws must degrade to an unauthenticated request, not crash the call.
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(TokenProvider { error("clerk not ready") }))
            .build()
        val api = ApiClient.service(mock.server.url("/").toString(), client)
        mock.json(200, """{"success":true,"data":[]}""")
        runBlocking { api.getSpecializations() }
        assertNull(mock.server.takeRequest().getHeader("Authorization"))
        mock.shutdown()
    }
}
