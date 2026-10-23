package com.example.clinicai.data.repository

import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.testing.MockApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DoctorRepositoryTest {
    private val mock = MockApi()
    private val repo = DoctorRepository(mock.api, mock.gson)

    @After fun tearDown() = runCatching { mock.shutdown() }.let { }

    private fun doctorsPage(vararg ids: String, page: Int, totalPages: Int) =
        """{"success":true,"data":[${ids.joinToString(",") { """{"id":"$it","name":"Dr $it"}""" }}],"meta":{"page":$page,"totalPages":$totalPages}}"""

    @Test fun allDoctorsFollowsPagination() {
        mock.json(200, doctorsPage("a", "b", page = 1, totalPages = 2))
        mock.json(200, doctorsPage("c", page = 2, totalPages = 2))
        val all = (runBlocking { repo.allDoctors() } as ApiResult.Success).data
        assertEquals(listOf("a", "b", "c"), all.map { it.id })
        assertTrue(mock.server.takeRequest().path!!.contains("page=1"))
        assertTrue(mock.server.takeRequest().path!!.contains("page=2"))
    }

    @Test fun doctorFallsBackToSearchWhileTheByIdRouteIsNotDeployed() {
        mock.html(405)
        mock.json(200, doctorsPage("a", "b", page = 1, totalPages = 1))
        val doctor = (runBlocking { repo.doctor("b") } as ApiResult.Success).data
        assertEquals("b", doctor.id)
    }

    @Test fun doctorUsesTheByIdRouteWhenAvailable() {
        mock.json(200, """{"success":true,"data":{"id":"b","name":"Dr B"}}""")
        val doctor = (runBlocking { repo.doctor("b") } as ApiResult.Success).data
        assertEquals("Dr B", doctor.name)
        assertEquals("/api/doctors/b", mock.server.takeRequest().path)
    }

    @Test fun unknownDoctorInFallbackIsNotFound() {
        mock.html(404)
        mock.json(200, doctorsPage("a", page = 1, totalPages = 1))
        val error = (runBlocking { repo.doctor("zzz") } as ApiResult.Failure).error
        assertTrue(error is AppError.NotFound)
    }

    @Test fun partnerClinicsAreDistinctAndSorted() {
        mock.json(
            200,
            """{"success":true,"data":[
              {"id":"1","name":"A","clinic":{"id":"c2","name":"Zeta Hospital","qrToken":"t2"}},
              {"id":"2","name":"B","clinic":{"id":"c1","name":"Alpha Hospital","qrToken":"t1"}},
              {"id":"3","name":"C","clinic":{"id":"c1","name":"Alpha Hospital","qrToken":"t1"}}],
              "meta":{"page":1,"totalPages":1}}""",
        )
        val clinics = (runBlocking { repo.partnerClinics() } as ApiResult.Success).data
        assertEquals(listOf("Alpha Hospital", "Zeta Hospital"), clinics.map { it.name })
        assertEquals("t1", clinics.first().qrToken)
    }

    @Test fun availabilityPassesTheDateQuery() {
        mock.json(200, """{"success":true,"data":{"date":"2026-10-26","slots":["10:00","10:15"]}}""")
        val data = (runBlocking { repo.availability("doc_01", "2026-10-26") } as ApiResult.Success).data
        assertEquals(listOf("10:00", "10:15"), data.slots)
        assertEquals("/api/doctors/doc_01/availability?date=2026-10-26", mock.server.takeRequest().path)
    }
}
