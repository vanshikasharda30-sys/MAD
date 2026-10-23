package com.example.clinicai.data.repository

import com.example.clinicai.data.model.Appointment
import com.example.clinicai.data.model.BookAppointmentRequest
import com.example.clinicai.data.model.CancelResult
import com.example.clinicai.data.model.Review
import com.example.clinicai.data.model.ReviewRequest
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.ApiService
import com.example.clinicai.data.remote.safeApiCall
import com.example.clinicai.domain.AppointmentBooker
import com.google.gson.Gson

class AppointmentRepository(private val api: ApiService, private val gson: Gson) : AppointmentBooker {

    /**
     * Sends the booking to the backend. A [ApiResult.Success] means the server created the row and
     * returned its id; every other outcome (slot taken, validation, 401, timeout, offline) is a
     * [ApiResult.Failure] and must never be shown as a confirmation.
     */
    override suspend fun book(request: BookAppointmentRequest): ApiResult<Appointment> =
        safeApiCall(gson) { api.bookAppointment(request) }.let { result ->
            if (result is ApiResult.Success && result.data.id.isBlank()) {
                ApiResult.Failure(com.example.clinicai.data.remote.AppError.InvalidResponse)
            } else {
                result
            }
        }

    suspend fun list(): ApiResult<List<Appointment>> = safeApiCall(gson) { api.listAppointments(null) }

    suspend fun get(id: String): ApiResult<Appointment> = safeApiCall(gson) { api.getAppointment(id) }

    suspend fun cancel(id: String): ApiResult<CancelResult> = safeApiCall(gson) { api.cancelAppointment(id) }

    suspend fun review(appointmentId: String, rating: Int, text: String?): ApiResult<Review> =
        safeApiCall(gson) { api.createReview(ReviewRequest(appointmentId, rating, text?.takeIf { it.isNotBlank() })) }
}
