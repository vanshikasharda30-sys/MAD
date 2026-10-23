package com.example.clinicai.ui.common

import android.content.Context
import com.example.clinicai.R
import com.example.clinicai.data.remote.AppError

/** Turns an [AppError] into text a patient can act on. Server-supplied messages win when present. */
fun AppError.toMessage(context: Context): String = when (this) {
    AppError.Offline -> context.getString(R.string.error_offline)
    AppError.ServerUnreachable -> context.getString(R.string.error_server_unreachable)
    AppError.SecureConnectionFailed -> context.getString(R.string.error_secure_connection)
    AppError.Timeout -> context.getString(R.string.error_timeout)
    is AppError.Unauthorized -> context.getString(R.string.error_unauthorized)
    is AppError.AccountNotProvisioned -> serverMessage ?: context.getString(R.string.error_unauthorized)
    is AppError.Forbidden -> serverMessage ?: context.getString(R.string.error_forbidden)
    is AppError.NotFound -> serverMessage ?: context.getString(R.string.error_generic)
    is AppError.Conflict -> serverMessage ?: context.getString(R.string.book_error_slot_taken)
    is AppError.Validation -> serverMessage ?: context.getString(R.string.error_generic)
    AppError.EndpointMissing -> context.getString(R.string.error_not_deployed)
    AppError.InvalidResponse -> context.getString(R.string.error_invalid_response)
    is AppError.Server -> serverMessage ?: context.getString(R.string.error_generic)
    is AppError.Unknown -> serverMessage ?: context.getString(R.string.error_generic)
}
