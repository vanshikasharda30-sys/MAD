package com.example.clinicai.data.repository

import com.example.clinicai.data.model.Appointment
import com.example.clinicai.data.model.PaymentOrder
import com.example.clinicai.data.model.VerifyPaymentRequest
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.ApiService
import com.example.clinicai.data.remote.safeApiCall
import com.google.gson.Gson

/** Optional booking deposit, via the backend's Razorpay order + signature verification. */
class PaymentRepository(private val api: ApiService, private val gson: Gson) {

    suspend fun createOrder(appointmentId: String): ApiResult<PaymentOrder> =
        safeApiCall(gson) { api.createPaymentOrder(appointmentId) }

    suspend fun verify(appointmentId: String, request: VerifyPaymentRequest): ApiResult<Appointment> =
        safeApiCall(gson) { api.verifyPayment(appointmentId, request) }
}
