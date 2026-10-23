package com.example.clinicai.data.remote

import com.google.gson.JsonObject

/** Every ClinicAI endpoint answers `{ success, data, meta? }` or `{ success: false, error }`. */
data class ApiEnvelope<T>(
    val success: Boolean = false,
    val data: T? = null,
    val meta: JsonObject? = null,
    val error: ApiErrorBody? = null,
)

data class ApiErrorBody(
    val code: String? = null,
    val message: String? = null,
    val details: com.google.gson.JsonElement? = null,
)
