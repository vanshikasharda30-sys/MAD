package com.example.clinicai.data.repository

import com.example.clinicai.data.model.BookAppointmentRequest
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.testing.MockApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentRepositoryTest {
    private val mock = MockApi(token = "jwt")
    private val repo = AppointmentRepository(mock.api, mock.gson)
    private val request = BookAppointmentRequest("doc_01", "2026-10-26", "10:00", "fever")

    @After fun tearDown() = runCatching { mock.shutdown() }.let { }

    @Test fun successfulBookingReturnsTheServerIssuedAppointment() {
        mock.json(201, MockApi.APPOINTMENT_CREATED)
        val result = runBlocking { repo.book(request) }

        assertTrue(result is ApiResult.Success)
        val appointment = (result as ApiResult.Success).data
        assertEquals("cm_appt_001", appointment.id)
        assertEquals("confirmed", appointment.status)
        assertEquals("10:00", appointment.startTime)
    }

    @Test fun sendsTheDocumentedPayloadToThePostEndpoint() {
        mock.json(201, MockApi.APPOINTMENT_CREATED)
        runBlocking { repo.book(request) }
        val recorded = mock.server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/appointments", recorded.path)
        assertEquals("Bearer jwt", recorded.getHeader("Authorization"))
        val body = recorded.body.readUtf8()
        assertTrue(body, body.contains("\"doctorId\":\"doc_01\""))
        assertTrue(body, body.contains("\"date\":\"2026-10-26\""))
        assertTrue(body, body.contains("\"startTime\":\"10:00\""))
        assertTrue(body, body.contains("\"notes\":\"fever\""))
    }

    @Test fun slotTakenIsAFailureWithTheServersMessage() {
        mock.json(400, """{"success":false,"error":{"code":"VALIDATION_ERROR","message":"That slot was just booked by someone else — please pick another"}}""")
        val result = runBlocking { repo.book(request) }
        assertTrue(result is ApiResult.Failure)
        val error = (result as ApiResult.Failure).error
        assertTrue(error is AppError.Validation)
        assertTrue(error.serverMessage!!.contains("just booked"))
    }

    @Test fun simultaneousBookingLoserGetsAConflict() {
        mock.json(409, """{"success":false,"error":{"code":"SLOT_CONFLICT","message":"That slot is not available"}}""")
        val error = (runBlocking { repo.book(request) } as ApiResult.Failure).error
        assertTrue(error is AppError.Conflict)
        assertEquals("That slot is not available", error.serverMessage)
    }

    @Test fun signedOutBookingIsAnAuthFailure() {
        mock.json(401, """{"success":false,"error":{"code":"UNAUTHORIZED","message":"Sign-in required"}}""")
        val error = (runBlocking { repo.book(request) } as ApiResult.Failure).error
        assertTrue(error.isAuthProblem)
    }

    @Test fun staffAccountIsRefusedByTheServer() {
        mock.json(404, """{"success":false,"error":{"code":"NOT_FOUND","message":"Not permitted"}}""")
        val error = (runBlocking { repo.book(request) } as ApiResult.Failure).error
        assertTrue(error is AppError.NotFound)
        assertEquals("Not permitted", error.serverMessage)
    }

    @Test fun serverCrashIsAFailure() {
        mock.json(500, """{"success":false,"error":{"code":"INTERNAL_ERROR","message":"Something went wrong"}}""")
        assertTrue(runBlocking { repo.book(request) } is ApiResult.Failure)
    }

    @Test fun createdResponseWithoutAnIdIsNotTreatedAsConfirmed() {
        mock.json(201, """{"success":true,"data":{"id":"","doctorId":"doc_01","date":"2026-10-26T00:00:00.000Z"}}""")
        val result = runBlocking { repo.book(request) }
        assertEquals(AppError.InvalidResponse, (result as ApiResult.Failure).error)
    }

    @Test fun listsAppointmentsWithEmbeddedDoctor() {
        mock.json(
            200,
            """{"success":true,"data":[{"id":"a1","doctorId":"d1","type":"SCHEDULED","status":"confirmed","date":"2026-10-26T00:00:00.000Z",
               "startTime":"10:00","doctor":{"id":"d1","name":"Dr. X","consultationFee":500,"clinic":{"id":"c1","name":"C"},"specialization":{"id":"s","name":"GP"}}}]}""",
        )
        val list = (runBlocking { repo.list() } as ApiResult.Success).data
        assertEquals("Dr. X", list.single().doctor?.name)
        assertEquals("GP", list.single().doctor?.specializationName)
    }

    @Test fun cancelHitsTheCancelEndpoint() {
        mock.json(200, """{"success":true,"data":{"cancelled":true}}""")
        val result = runBlocking { repo.cancel("a1") }
        assertTrue(result is ApiResult.Success)
        val recorded = mock.server.takeRequest()
        assertEquals("PATCH", recorded.method)
        assertEquals("/api/appointments/a1/cancel", recorded.path)
    }

    @Test fun reviewSendsRatingAndOmitsBlankComment() {
        mock.json(201, """{"success":true,"data":{"id":"r1","rating":5}}""")
        runBlocking { repo.review("a1", 5, "  ") }
        val body = mock.server.takeRequest().body.readUtf8()
        assertTrue(body, body.contains("\"rating\":5"))
        assertTrue(body, !body.contains("reviewText"))
    }
}
