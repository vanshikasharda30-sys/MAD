package com.example.clinicai.data.repository

import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.testing.MockApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRepositoryTest {
    private val mock = MockApi()
    private val doctors = DoctorRepository(mock.api, mock.gson)
    private val chat = ChatRepository(mock.api, mock.gson, doctors)

    @After fun tearDown() = runCatching { mock.shutdown() }.let { }

    @Test fun postsClinicAndTrimmedMessageToTheChatEndpoint() {
        mock.json(200, """{"success":true,"data":{"type":"greeting","reply":"Hi!"}}""")
        runBlocking { chat.send("clinic_01", "  hello ") }
        val recorded = mock.server.takeRequest()
        assertEquals("/api/chat", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body, body.contains("\"clinicId\":\"clinic_01\""))
        assertTrue(body, body.contains("\"message\":\"hello\""))
    }

    @Test fun parsesSymptomMatch() {
        mock.json(200, """{"success":true,"data":{"type":"symptom_match","specializationId":"spec_gp","specializationName":"General Physician","reply":"See a GP"}}""")
        val reply = (runBlocking { chat.send("clinic_01", "fever") } as ApiResult.Success).data
        assertEquals("symptom_match", reply.type)
        assertEquals("spec_gp", reply.specializationId)
        assertEquals("See a GP", reply.reply)
    }

    @Test fun suggestedDoctorsComeFromTheSearchEndpointFilteredByClinicAndSpecialty() {
        mock.json(200, """{"success":true,"data":[{"id":"d1","name":"Dr. A","consultationFee":500,"avgRating":5,"reviewCount":3}],"meta":{"page":1,"totalPages":1}}""")
        val doctorsList = runBlocking { chat.suggestedDoctors("spec_gp", "clinic_01") }
        assertEquals("Dr. A", doctorsList.single().name)
        val path = mock.server.takeRequest().path.orEmpty()
        assertTrue(path, path.contains("specializationId=spec_gp"))
        assertTrue(path, path.contains("clinicId=clinic_01"))
        assertTrue(path, path.contains("pageSize=2"))
    }

    @Test fun suggestedDoctorsFailureDegradesToEmptyList() {
        mock.html(500)
        assertTrue(runBlocking { chat.suggestedDoctors("spec_gp", "clinic_01") }.isEmpty())
    }
}
