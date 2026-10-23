package com.example.clinicai.testing

import com.example.clinicai.data.remote.ApiClient
import com.example.clinicai.data.remote.ApiService
import com.example.clinicai.data.remote.TokenProvider
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

/** A real Retrofit + OkHttp + Gson stack pointed at a local MockWebServer. */
class MockApi(token: String? = null, readTimeoutMs: Long = 5_000) {
    val server = MockWebServer().apply { start() }
    val gson = ApiClient.gson
    val api: ApiService

    init {
        val client = OkHttpClient.Builder()
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .connectTimeout(2, TimeUnit.SECONDS)
            .addInterceptor(com.example.clinicai.data.remote.AuthInterceptor(TokenProvider { token }))
            .build()
        api = ApiClient.service(server.url("/").toString(), client, gson)
    }

    fun json(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))
    }

    fun html(code: Int, body: String = "<html>not found</html>") {
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "text/html").setBody(body))
    }

    fun shutdown() = server.shutdown()

    companion object {
        const val APPOINTMENT_CREATED = """{"success":true,"data":{"id":"cm_appt_001","patientId":"p1","doctorId":"doc_01",
            "type":"SCHEDULED","status":"confirmed","date":"2026-10-26T00:00:00.000Z","startTime":"10:00","notes":"fever",
            "createdAt":"2026-10-23T10:15:00.000Z","depositAmount":null,"depositStatus":"PENDING"}}"""
    }
}
