package com.example.clinicai.data.remote

/** Everything that can go wrong talking to the backend, normalised for the UI. */
sealed class AppError(open val serverMessage: String? = null) {
    /** The host name couldn't be resolved: typically no internet connection at all. */
    data object Offline : AppError()

    /** Internet works but the server didn't accept the connection (refused, no route, reset): it is down or the URL is wrong. */
    data object ServerUnreachable : AppError()

    /** The TLS handshake failed (bad certificate, wrong date on the device, interception). */
    data object SecureConnectionFailed : AppError()

    data object Timeout : AppError()

    /** 401 — no or expired session. */
    data class Unauthorized(override val serverMessage: String? = null) : AppError(serverMessage)

    /** 401 `ACCOUNT_NOT_PROVISIONED` — Clerk account exists but has no Patient/Staff row yet. */
    data class AccountNotProvisioned(override val serverMessage: String? = null) : AppError(serverMessage)

    data class Forbidden(override val serverMessage: String? = null) : AppError(serverMessage)

    data class NotFound(override val serverMessage: String? = null) : AppError(serverMessage)

    /** 400 — e.g. slot taken, invalid payload. [serverMessage] is user-presentable. */
    data class Validation(override val serverMessage: String? = null) : AppError(serverMessage)

    /** 409 — the slot was taken by someone else (or is otherwise no longer bookable). */
    data class Conflict(override val serverMessage: String? = null) : AppError(serverMessage)

    /** 404/405 with a non-JSON body: the route doesn't exist on this deployment yet. */
    data object EndpointMissing : AppError()

    data class Server(val httpCode: Int, override val serverMessage: String? = null) : AppError(serverMessage)

    /** 2xx but the body wasn't the documented envelope. */
    data object InvalidResponse : AppError()

    data class Unknown(override val serverMessage: String? = null) : AppError(serverMessage)

    val isAuthProblem: Boolean get() = this is Unauthorized || this is AccountNotProvisioned
}
