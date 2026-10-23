package com.example.clinicai.data.repository

import com.example.clinicai.data.model.ChatReply
import com.example.clinicai.data.model.ChatRequest
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.ApiService
import com.example.clinicai.data.remote.safeApiCall
import com.google.gson.Gson

/**
 * Talks to `POST /api/chat`. The backend is rule-based (keyword triage + FAQ matching) with an
 * optional server-side LLM classifier; the app displays exactly what it returns and keeps no
 * server-side conversation state because the endpoint is stateless.
 */
class ChatRepository(
    private val api: ApiService,
    private val gson: Gson,
    private val doctors: DoctorRepository,
) {
    suspend fun send(clinicId: String, message: String): ApiResult<ChatReply> =
        safeApiCall(gson) { api.chat(ChatRequest(clinicId, message.trim())) }

    /** Top-rated doctors for the specialization the chat recommended, as the website's chat widget shows. */
    suspend fun suggestedDoctors(specializationId: String, clinicId: String): List<Doctor> =
        when (val result = doctors.search(specializationId = specializationId, clinicId = clinicId, pageSize = 2)) {
            is ApiResult.Success -> result.data
            is ApiResult.Failure -> emptyList()
        }
}
