package com.example.clinicai.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface PaymentResult {
    data class Success(val orderId: String, val paymentId: String, val signature: String) : PaymentResult
    data class Failure(val code: Int, val description: String?) : PaymentResult
}

/**
 * The Razorpay SDK reports payment outcomes to the hosting Activity. The Activity forwards them
 * here so the screen's view model can verify the payment with the backend.
 */
class PaymentResultBus {
    private val _results = MutableSharedFlow<PaymentResult>(extraBufferCapacity = 4)
    val results: SharedFlow<PaymentResult> = _results.asSharedFlow()

    fun publish(result: PaymentResult) {
        _results.tryEmit(result)
    }
}
