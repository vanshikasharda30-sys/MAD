package com.example.clinicai.data.remote

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.MalformedJsonException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import retrofit2.Response

/**
 * Runs a Retrofit call and converts every outcome — success envelope, error envelope, HTML 404 from
 * a missing route, malformed JSON, timeouts and connectivity loss — into an [ApiResult]. Callers
 * never see an exception, and a failure is never reported as a success.
 */
suspend fun <T> safeApiCall(
    gson: Gson,
    call: suspend () -> Response<ApiEnvelope<T>>,
): ApiResult<T> {
    val response = try {
        call()
    } catch (e: CancellationException) {
        throw e
    } catch (e: SocketTimeoutException) {
        return ApiResult.Failure(AppError.Timeout)
    } catch (e: MalformedJsonException) {
        // An IOException subtype, but the connection worked: the body just isn't valid JSON.
        return ApiResult.Failure(AppError.InvalidResponse)
    } catch (e: JsonParseException) {
        return ApiResult.Failure(AppError.InvalidResponse)
    } catch (e: UnknownHostException) {
        return ApiResult.Failure(AppError.Offline)
    } catch (e: SSLException) {
        return ApiResult.Failure(AppError.SecureConnectionFailed)
    } catch (e: IOException) {
        // ConnectException, NoRouteToHostException, connection reset…: the network is up, the server isn't reachable.
        return ApiResult.Failure(AppError.ServerUnreachable)
    } catch (e: RuntimeException) {
        // Retrofit wraps converter failures; treat them as an unreadable response.
        return ApiResult.Failure(AppError.InvalidResponse)
    }

    return if (response.isSuccessful) {
        val envelope = response.body()
        val data = envelope?.data
        if (envelope != null && envelope.success && data != null) {
            ApiResult.Success(data, envelope.meta)
        } else {
            ApiResult.Failure(envelope?.error?.let { mapErrorBody(response.code(), it) } ?: AppError.InvalidResponse)
        }
    } else {
        ApiResult.Failure(parseHttpError(gson, response))
    }
}

private fun parseHttpError(gson: Gson, response: Response<*>): AppError {
    val raw = try {
        response.errorBody()?.string().orEmpty()
    } catch (e: IOException) {
        ""
    }
    val envelope = try {
        if (raw.trimStart().startsWith("{")) gson.fromJson(raw, ApiEnvelope::class.java) else null
    } catch (e: JsonSyntaxException) {
        null
    }

    val body = envelope?.error
    if (body != null) return mapErrorBody(response.code(), body)

    return when (response.code()) {
        401 -> AppError.Unauthorized()
        403 -> AppError.Forbidden()
        404, 405 -> AppError.EndpointMissing
        in 500..599 -> AppError.Server(response.code())
        else -> AppError.Unknown(null)
    }
}

internal fun mapErrorBody(httpCode: Int, body: ApiErrorBody): AppError = when {
    body.code == "ACCOUNT_NOT_PROVISIONED" -> AppError.AccountNotProvisioned(body.message)
    httpCode == 401 || body.code == "UNAUTHORIZED" -> AppError.Unauthorized(body.message)
    httpCode == 409 || body.code == "SLOT_CONFLICT" -> AppError.Conflict(body.message)
    httpCode == 403 || body.code == "FORBIDDEN" -> AppError.Forbidden(body.message)
    httpCode == 404 || body.code == "NOT_FOUND" -> AppError.NotFound(body.message)
    httpCode == 400 || body.code == "VALIDATION_ERROR" -> AppError.Validation(body.message)
    httpCode in 500..599 -> AppError.Server(httpCode, body.message)
    else -> AppError.Unknown(body.message)
}
