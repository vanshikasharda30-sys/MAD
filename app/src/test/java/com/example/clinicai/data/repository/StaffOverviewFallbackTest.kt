package com.example.clinicai.data.repository

import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.testing.MockApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The staff overview must show real figures even when the deployed backend predates
 * `GET /api/dashboard/overview` (it answers an HTML 404), and must never show a partial number.
 */
class StaffOverviewFallbackTest {
    private val mock = MockApi(token = "jwt")
    private val repo = StaffRepository(mock.api, mock.gson)

    @After fun tearDown() = runCatching { mock.shutdown() }.let { }

    private fun json(code: Int, body: String) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private fun html404() = MockResponse().setResponseCode(404).setHeader("Content-Type", "text/html").setBody("<html>404</html>")

    private fun serve(queueFor: (String) -> MockResponse, doctors: MockResponse = json(200, DOCTORS)) {
        mock.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return when {
                    path.startsWith("/api/dashboard/overview") -> html404()
                    path.startsWith("/api/clinics/me") -> json(200, CLINIC)
                    path.startsWith("/api/doctors/search") -> doctors
                    path.startsWith("/api/queue/") -> queueFor(path.substringAfter("/api/queue/").substringBefore("/"))
                    else -> MockResponse().setResponseCode(500)
                }
            }
        }
    }

    private fun queue(id: String, waiting: Int) = json(
        200,
        """{"success":true,"data":{"doctorId":"$id","waiting":[${
            (1..waiting).joinToString(",") { """{"queueEntryId":"q$it","queueNumber":$it,"position":$it,"estimatedWaitMinutes":${it * 10}}""" }
        }]}}""",
    )

    @Test fun usesTheDedicatedEndpointWhenTheServerHasIt() {
        mock.json(
            200,
            """{"success":true,"data":{"clinic":{"id":"c1","name":"Clinic","area":"A","city":"C"},"totalDoctorCount":3,
                "activeDoctors":[],"activeDoctorCount":2,"queueWaiting":4,"avgWaitMinutes":12}}""",
        )
        val data = (runBlocking { repo.overview("c1") } as ApiResult.Success).data
        assertEquals(4, data.queueWaiting)
        assertEquals(12, data.avgWaitMinutes)
        assertEquals(1, mock.server.requestCount)
    }

    @Test fun composesRealFiguresWhenTheDashboardRouteIsMissing() {
        serve({ id -> if (id == "doc_a") queue(id, 2) else queue(id, 3) })
        val result = runBlocking { repo.overview("c1") }

        assertTrue(result.toString(), result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals("Demo Clinic", data.clinic?.name)
        assertEquals(2, data.activeDoctorCount)
        assertEquals(listOf("Dr. A", "Dr. B"), data.activeDoctors.map { it.name })
        assertEquals(5, data.queueWaiting)
        assertNull("an average wait must not be invented", data.avgWaitMinutes)
    }

    @Test fun anEmptyQueueIsZeroNotAnError() {
        serve({ id -> queue(id, 0) })
        val data = (runBlocking { repo.overview("c1") } as ApiResult.Success).data
        assertEquals(0, data.queueWaiting)
    }

    @Test fun aFailingQueueRequestFailsTheWholeOverviewInsteadOfShowingAPartialCount() {
        serve({ id -> if (id == "doc_a") queue(id, 2) else json(500, """{"success":false,"error":{"code":"INTERNAL","message":"boom"}}""") })
        val result = runBlocking { repo.overview("c1") }
        assertTrue(result is ApiResult.Failure)
        assertTrue((result as ApiResult.Failure).error is AppError.Server)
    }

    @Test fun anUnauthorisedCallerStaysUnauthorised() {
        mock.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.path.orEmpty().startsWith("/api/dashboard/overview")) html404()
                else json(403, """{"success":false,"error":{"code":"FORBIDDEN","message":"Staff only"}}""")
        }
        val result = runBlocking { repo.overview("c1") }
        assertTrue(result is ApiResult.Failure)
        assertTrue((result as ApiResult.Failure).error !is AppError.EndpointMissing)
    }

    @Test fun everyRequestCarriesTheSessionTokenAndNoClientSuppliedRole() {
        serve({ id -> queue(id, 1) })
        runBlocking { repo.overview("c1") }
        repeat(mock.server.requestCount) {
            val r = mock.server.takeRequest()
            assertEquals("Bearer jwt", r.getHeader("Authorization"))
            assertTrue(r.path.orEmpty(), !r.path.orEmpty().contains("role", ignoreCase = true))
        }
    }

    private companion object {
        const val CLINIC = """{"success":true,"data":{"id":"c1","name":"Demo Clinic","area":"Bandra","city":"Mumbai"}}"""
        const val DOCTORS = """{"success":true,"data":[
            {"id":"doc_b","name":"Dr. B","specialization":{"id":"s1","name":"ENT"}},
            {"id":"doc_a","name":"Dr. A","specialization":{"id":"s2","name":"GP"}}]}"""
    }
}
