package com.example.clinicai.data.auth

import org.junit.Assert.assertEquals
import org.junit.Test

/** The codes below are the ones the real Clerk instance returned when probed. */
class AuthErrorClassifierTest {
    private fun kind(code: String?, http: Int? = 422) = AuthErrorClassifier.classify(code, http)

    @Test fun wrongPasswordAndUnknownAccountLookIdentical() {
        assertEquals(AuthFailureKind.INVALID_CREDENTIALS, kind("form_password_incorrect"))
        assertEquals(AuthFailureKind.INVALID_CREDENTIALS, kind("form_identifier_not_found"))
    }

    @Test fun codeProblemsAreDistinguished() {
        assertEquals(AuthFailureKind.CODE_INVALID, kind("form_code_incorrect"))
        assertEquals(AuthFailureKind.CODE_EXPIRED, kind("verification_expired"))
        assertEquals(AuthFailureKind.TOO_MANY_ATTEMPTS, kind("verification_failed"))
    }

    @Test fun rateLimitingIsRecognisedByStatusOrCode() {
        assertEquals(AuthFailureKind.RATE_LIMITED, kind("too_many_requests", 429))
        assertEquals(AuthFailureKind.RATE_LIMITED, kind(null, 429))
        assertEquals(AuthFailureKind.RATE_LIMITED, kind("too_many_requests", null))
    }

    @Test fun duplicateAccountIsRecognised() {
        assertEquals(AuthFailureKind.ACCOUNT_EXISTS, kind("form_identifier_exists"))
    }

    @Test fun passwordPolicyRejectionsUseClerksOwnMessage() {
        assertEquals(AuthFailureKind.PASSWORD_REJECTED, kind("form_password_length_too_short"))
        assertEquals(AuthFailureKind.PASSWORD_REJECTED, kind("form_password_pwned"))
        assertEquals(AuthFailureKind.PASSWORD_REJECTED, kind("form_password_not_strong_enough"))
    }

    @Test fun expiredFlowsAndUnknownCodes() {
        assertEquals(AuthFailureKind.FLOW_EXPIRED, kind("resource_not_found"))
        assertEquals(AuthFailureKind.GENERIC, kind("something_new"))
        assertEquals(AuthFailureKind.GENERIC, kind(null, null))
    }
}
