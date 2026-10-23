package com.example.clinicai.data.repository

import com.example.clinicai.data.model.Clinic
import com.example.clinicai.data.model.Faq
import com.example.clinicai.data.model.UpdateClinicRequest
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.ApiService
import com.example.clinicai.data.remote.safeApiCall
import com.google.gson.Gson

class ClinicRepository(private val api: ApiService, private val gson: Gson) {

    suspend fun faqs(clinicId: String): ApiResult<List<Faq>> = safeApiCall(gson) { api.getClinicFaqs(clinicId) }

    /** Resolves a walk-in QR token to its clinic and the doctors accepting walk-ins. */
    suspend fun resolveQr(token: String): ApiResult<Clinic> = safeApiCall(gson) { api.resolveClinicQr(token) }

    suspend fun myClinic(): ApiResult<Clinic> = safeApiCall(gson) { api.getMyClinic() }

    suspend fun updateMyClinic(phone: String, email: String): ApiResult<Clinic> =
        safeApiCall(gson) { api.updateMyClinic(UpdateClinicRequest(phone = phone, email = email)) }
}
