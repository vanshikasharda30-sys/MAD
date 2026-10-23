package com.example.clinicai.data.auth

/** What the app knows about the signed-in state right now. Drives every screen that cares about it. */
sealed interface AuthState {
    /** The Clerk SDK is still loading / restoring a saved session. Don't show "sign in" yet. */
    data object Initializing : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val userId: String) : AuthState
}

val AuthState.isSignedIn: Boolean get() = this is AuthState.SignedIn

/** Why a one-time code is being asked for. Decides which Clerk call verifies/re-sends it. */
enum class CodePurpose {
    /** New account: confirm the e-mail address. */
    SIGN_UP_EMAIL,

    /** Sign in with an e-mailed code instead of a password. */
    SIGN_IN_EMAIL,

    /** Extra check after a correct password: Clerk "client trust" (new device) or MFA, by e-mail. */
    SECOND_FACTOR_EMAIL,
    SECOND_FACTOR_PHONE,

    /** Authenticator-app code (nothing is sent, so nothing can be re-sent). */
    SECOND_FACTOR_TOTP,

    /** Forgot password: the code that proves the user owns the e-mail address. */
    PASSWORD_RESET,
}

data class CodeChallenge(
    val purpose: CodePurpose,
    /** Where the code went, as Clerk reports it (e.g. an e-mail address). Null when unknown. */
    val destination: String?,
) {
    val canResend: Boolean get() = purpose != CodePurpose.SECOND_FACTOR_TOTP
}

enum class AuthFailureKind {
    GENERIC,
    NETWORK,
    NATIVE_API_DISABLED,
    INVALID_CREDENTIALS,
    CODE_INVALID,
    CODE_EXPIRED,
    TOO_MANY_ATTEMPTS,
    RATE_LIMITED,
    ACCOUNT_EXISTS,
    PASSWORD_REJECTED,
    FLOW_EXPIRED,
}

/** Result of one step of sign-in / sign-up / password reset. */
sealed interface AuthOutcome {
    /** A session is active. */
    data object SignedIn : AuthOutcome

    /** A one-time code was sent and must be entered next. */
    data class NeedsCode(val challenge: CodeChallenge) : AuthOutcome

    /** The user must choose a new password (forgot-password, or Clerk requires a change at sign-in). */
    data object NeedsNewPassword : AuthOutcome

    /** Forgot-password finished: the password was changed and the user must sign in again. */
    data object PasswordResetDone : AuthOutcome

    /** Clerk asked for a step this app version can't perform. [status] is Clerk's raw status name. */
    data class Unsupported(val status: String) : AuthOutcome

    data class Failed(val message: String?, val kind: AuthFailureKind = AuthFailureKind.GENERIC) : AuthOutcome
}

data class AccountInfo(
    val firstName: String?,
    val lastName: String?,
    val email: String?,
) {
    val displayName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ").trim()
}
