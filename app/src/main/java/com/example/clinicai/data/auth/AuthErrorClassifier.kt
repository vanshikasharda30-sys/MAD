package com.example.clinicai.data.auth

/** Maps Clerk's error codes (and HTTP status) to the kinds of failure the UI explains. Pure, so it is unit-tested. */
object AuthErrorClassifier {

    fun classify(code: String?, httpStatus: Int? = null): AuthFailureKind {
        if (httpStatus == 429) return AuthFailureKind.RATE_LIMITED
        val c = code.orEmpty()
        return when {
            c == "too_many_requests" || c == "rate_limit_exceeded" -> AuthFailureKind.RATE_LIMITED

            // Unknown e-mail and wrong password share one kind on purpose: the UI must not reveal which.
            c == "form_password_incorrect" || c == "form_identifier_not_found" ||
                c == "strategy_for_user_invalid" || c == "form_password_validation_failed" -> AuthFailureKind.INVALID_CREDENTIALS

            c == "form_code_incorrect" -> AuthFailureKind.CODE_INVALID
            c == "verification_expired" -> AuthFailureKind.CODE_EXPIRED
            c == "verification_failed" || c == "verification_already_verified" -> AuthFailureKind.TOO_MANY_ATTEMPTS

            c == "form_identifier_exists" -> AuthFailureKind.ACCOUNT_EXISTS

            c.startsWith("form_password_") -> AuthFailureKind.PASSWORD_REJECTED

            c == "resource_not_found" || c == "sign_in_not_found" || c == "sign_up_not_found" ||
                c == "client_state_invalid" || c == "sign_in_status_invalid" -> AuthFailureKind.FLOW_EXPIRED

            c.contains("native", ignoreCase = true) -> AuthFailureKind.NATIVE_API_DISABLED
            else -> AuthFailureKind.GENERIC
        }
    }
}
