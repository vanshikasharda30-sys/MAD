package com.example.clinicai.data.model

import com.google.gson.annotations.SerializedName

/** `POST /api/appointments/{id}/payment/create-order` -> `data`. `amount` is in paise. */
data class PaymentOrder(
    val orderId: String,
    val amount: Int,
    val currency: String = "INR",
    val keyId: String? = null,
)

data class VerifyPaymentRequest(
    @SerializedName("razorpay_order_id") val razorpayOrderId: String,
    @SerializedName("razorpay_payment_id") val razorpayPaymentId: String,
    @SerializedName("razorpay_signature") val razorpaySignature: String,
)
