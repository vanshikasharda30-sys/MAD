package com.example.clinicai.data.auth

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.auth.types.MfaType
import com.clerk.api.auth.types.VerificationType
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.resetPassword
import com.clerk.api.signin.sendEmailCode as sendSignInEmailCode
import com.clerk.api.signin.sendMfaEmailCode
import com.clerk.api.signin.sendMfaPhoneCode
import com.clerk.api.signin.sendResetPasswordEmailCode
import com.clerk.api.signin.verifyCode as verifySignInCode
import com.clerk.api.signin.verifyMfaCode
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.sendEmailCode as sendSignUpEmailCode
import com.clerk.api.signup.verifyCode as verifySignUpCode
import com.example.clinicai.data.remote.TokenProvider
import com.example.clinicai.domain.AccountProvider
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

/**
 * Wrapper around the Clerk Android SDK. The website authenticates with Clerk, so the app signs in
 * with the same Clerk instance and sends the session JWT to the backend. Credentials go straight
 * to Clerk and are never stored by the app.
 *
 * Sign-in is a small state machine because Clerk may demand more than a password:
 *  - `needs_client_trust` / `needs_second_factor`: an e-mailed (or SMS / authenticator) code
 *  - `needs_new_password`: Clerk requires the password to be changed (e.g. found in a data breach)
 *  - `needs_first_factor`: fall back to an e-mailed sign-in code
 * Each step returns an [AuthOutcome] and the manager remembers where the flow is, so the next call
 * ([submitCode], [resendCode], [submitNewPassword]) continues it.
 */
class ClerkAuthManager(private val publishableKey: String) : TokenProvider, AccountProvider {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Reactive sign-in state. `Initializing` until the SDK has loaded and restored any saved session. */
    override val authState: StateFlow<AuthState> by lazy {
        combine(Clerk.isInitialized, Clerk.sessionFlow) { initialized, session ->
            when {
                !initialized -> AuthState.Initializing
                session != null && session.status == Session.SessionStatus.ACTIVE -> AuthState.SignedIn(session.user?.id.orEmpty())
                else -> AuthState.SignedOut
            }
        }.stateIn(scope, SharingStarted.Eagerly, AuthState.Initializing)
    }

    override val isSignedIn: Boolean get() = authState.value is AuthState.SignedIn

    /** Suspends until the SDK finished initialising, so callers never act on a not-yet-restored session. */
    override suspend fun awaitReady(): AuthState = authState.first { it !is AuthState.Initializing }

    // The flow currently in progress.
    private var pendingSignIn: SignIn? = null
    private var pendingSignUp: SignUp? = null
    private var challenge: CodeChallenge? = null
    private var forgotPasswordFlow = false

    fun initialize(context: Context) {
        Clerk.initialize(context.applicationContext, publishableKey)
    }

    /** The signed-in user's Clerk id, used to keep per-user local data apart. */
    val currentUserId: String? get() = (authState.value as? AuthState.SignedIn)?.userId?.takeIf { it.isNotBlank() }

    override fun accountInfo(): AccountInfo? {
        val user = Clerk.activeUser ?: return null
        return AccountInfo(
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.emailAddresses?.firstOrNull { it.id == user.primaryEmailAddressId }?.emailAddress
                ?: user.emailAddresses?.firstOrNull()?.emailAddress,
        )
    }

    override suspend fun currentToken(): String? {
        if (Clerk.activeSession == null) return null
        return when (val result = Clerk.auth.getToken()) {
            is ClerkResult.Success -> result.value
            is ClerkResult.Failure -> null
        }
    }

    // ---- Sign in --------------------------------------------------------------------------------

    suspend fun signIn(email: String, password: String): AuthOutcome = guarded {
        alreadySignedIn()?.let { return@guarded it }
        reset()
        when (val result = Clerk.auth.signInWithPassword {
            identifier = email
            this.password = password
        }) {
            is ClerkResult.Success -> handleSignIn(result.value)
            is ClerkResult.Failure -> failure(result)
        }
    }

    private suspend fun handleSignIn(signIn: SignIn): AuthOutcome {
        pendingSignIn = signIn
        return when (signIn.status) {
            SignIn.Status.COMPLETE -> activate(signIn.createdSessionId)
            SignIn.Status.NEEDS_CLIENT_TRUST, SignIn.Status.NEEDS_SECOND_FACTOR -> startSecondFactor(signIn)
            SignIn.Status.NEEDS_NEW_PASSWORD -> {
                forgotPasswordFlow = false
                AuthOutcome.NeedsNewPassword
            }
            SignIn.Status.NEEDS_FIRST_FACTOR -> startFirstFactorEmailCode(signIn)
            else -> AuthOutcome.Unsupported(signIn.statusRawValue)
        }
    }

    /** Client trust (new device) and MFA both finish with a code from one of the second factors. */
    private suspend fun startSecondFactor(signIn: SignIn): AuthOutcome {
        val factors = signIn.supportedSecondFactors.orEmpty()
        val email = factors.firstOrNull { it.strategy == "email_code" }
        val phone = factors.firstOrNull { it.strategy == "phone_code" }
        val totp = factors.firstOrNull { it.strategy == "totp" }
        return when {
            email != null -> when (val r = signIn.sendMfaEmailCode(email.emailAddressId)) {
                is ClerkResult.Success -> codeStep(r.value, CodeChallenge(CodePurpose.SECOND_FACTOR_EMAIL, email.safeIdentifier))
                is ClerkResult.Failure -> failure(r)
            }
            phone != null -> when (val r = signIn.sendMfaPhoneCode(phone.phoneNumberId)) {
                is ClerkResult.Success -> codeStep(r.value, CodeChallenge(CodePurpose.SECOND_FACTOR_PHONE, phone.safeIdentifier))
                is ClerkResult.Failure -> failure(r)
            }
            totp != null -> codeStep(signIn, CodeChallenge(CodePurpose.SECOND_FACTOR_TOTP, null))
            else -> AuthOutcome.Unsupported("second_factor:" + factors.joinToString(",") { it.strategy })
        }
    }

    private suspend fun startFirstFactorEmailCode(signIn: SignIn): AuthOutcome {
        val factor = signIn.supportedFirstFactors.orEmpty().firstOrNull { it.strategy == "email_code" }
            ?: return AuthOutcome.Unsupported("first_factor:" + signIn.supportedFirstFactors.orEmpty().joinToString(",") { it.strategy })
        return when (val r = signIn.sendSignInEmailCode(factor.emailAddressId)) {
            is ClerkResult.Success -> codeStep(r.value, CodeChallenge(CodePurpose.SIGN_IN_EMAIL, factor.safeIdentifier))
            is ClerkResult.Failure -> failure(r)
        }
    }

    private fun codeStep(signIn: SignIn, step: CodeChallenge): AuthOutcome {
        pendingSignIn = signIn
        challenge = step
        return AuthOutcome.NeedsCode(step)
    }

    // ---- Sign up --------------------------------------------------------------------------------

    suspend fun signUp(firstName: String, lastName: String, email: String, password: String): AuthOutcome = guarded {
        alreadySignedIn()?.let { return@guarded it }
        reset()
        when (val result = Clerk.auth.signUp {
            this.email = email
            this.password = password
            this.firstName = firstName
            this.lastName = lastName
        }) {
            is ClerkResult.Success -> {
                val signUp = result.value
                pendingSignUp = signUp
                if (signUp.status == SignUp.Status.COMPLETE) {
                    activate(signUp.createdSessionId)
                } else {
                    when (val sent = signUp.sendSignUpEmailCode()) {
                        is ClerkResult.Success -> {
                            pendingSignUp = sent.value
                            val step = CodeChallenge(CodePurpose.SIGN_UP_EMAIL, email)
                            challenge = step
                            AuthOutcome.NeedsCode(step)
                        }
                        is ClerkResult.Failure -> failure(sent)
                    }
                }
            }
            is ClerkResult.Failure -> failure(result)
        }
    }

    // ---- Codes ----------------------------------------------------------------------------------

    /** Verifies the code the user typed for whatever challenge is currently pending. */
    suspend fun submitCode(code: String): AuthOutcome = guarded {
        val step = challenge ?: return@guarded AuthOutcome.Failed(null, AuthFailureKind.FLOW_EXPIRED)
        val signIn = pendingSignIn
        when (step.purpose) {
            CodePurpose.SIGN_UP_EMAIL -> {
                val signUp = pendingSignUp ?: return@guarded AuthOutcome.Failed(null, AuthFailureKind.FLOW_EXPIRED)
                when (val r = signUp.verifySignUpCode(code, VerificationType.EMAIL)) {
                    is ClerkResult.Success -> {
                        pendingSignUp = r.value
                        if (r.value.status == SignUp.Status.COMPLETE) activate(r.value.createdSessionId)
                        else AuthOutcome.Unsupported("sign_up:" + r.value.status.name.lowercase())
                    }
                    is ClerkResult.Failure -> failure(r)
                }
            }
            CodePurpose.SIGN_IN_EMAIL -> signIn.verify { it.verifySignInCode(code) }
            CodePurpose.SECOND_FACTOR_EMAIL -> signIn.verify { it.verifyMfaCode(code, MfaType.EMAIL_CODE) }
            CodePurpose.SECOND_FACTOR_PHONE -> signIn.verify { it.verifyMfaCode(code, MfaType.PHONE_CODE) }
            CodePurpose.SECOND_FACTOR_TOTP -> signIn.verify { it.verifyMfaCode(code, MfaType.TOTP) }
            CodePurpose.PASSWORD_RESET -> {
                // No pending sign-in means the e-mail was unknown to Clerk. Answer exactly like a wrong code.
                val s = signIn ?: return@guarded AuthOutcome.Failed(null, AuthFailureKind.CODE_INVALID)
                when (val r = s.verifySignInCode(code)) {
                    is ClerkResult.Success -> {
                        pendingSignIn = r.value
                        if (r.value.status == SignIn.Status.NEEDS_NEW_PASSWORD) {
                            forgotPasswordFlow = true
                            AuthOutcome.NeedsNewPassword
                        } else {
                            handleSignIn(r.value)
                        }
                    }
                    is ClerkResult.Failure -> failure(r)
                }
            }
        }
    }

    private suspend fun SignIn?.verify(call: suspend (SignIn) -> ClerkResult<SignIn, ClerkErrorResponse>): AuthOutcome {
        val signIn = this ?: return AuthOutcome.Failed(null, AuthFailureKind.FLOW_EXPIRED)
        return when (val r = call(signIn)) {
            is ClerkResult.Success -> handleSignIn(r.value)
            is ClerkResult.Failure -> failure(r)
        }
    }

    /** Sends a fresh code for the pending challenge. Clerk enforces its own rate limit (reported as RATE_LIMITED). */
    suspend fun resendCode(): AuthOutcome = guarded {
        val step = challenge ?: return@guarded AuthOutcome.Failed(null, AuthFailureKind.FLOW_EXPIRED)
        val signIn = pendingSignIn
        when (step.purpose) {
            CodePurpose.SIGN_UP_EMAIL -> {
                val signUp = pendingSignUp ?: return@guarded AuthOutcome.Failed(null, AuthFailureKind.FLOW_EXPIRED)
                when (val r = signUp.sendSignUpEmailCode()) {
                    is ClerkResult.Success -> { pendingSignUp = r.value; AuthOutcome.NeedsCode(step) }
                    is ClerkResult.Failure -> failure(r)
                }
            }
            CodePurpose.SIGN_IN_EMAIL -> signIn.resend(step) { it.sendSignInEmailCode() }
            CodePurpose.SECOND_FACTOR_EMAIL -> signIn.resend(step) { it.sendMfaEmailCode() }
            CodePurpose.SECOND_FACTOR_PHONE -> signIn.resend(step) { it.sendMfaPhoneCode() }
            CodePurpose.SECOND_FACTOR_TOTP -> AuthOutcome.NeedsCode(step)
            CodePurpose.PASSWORD_RESET ->
                if (signIn == null) AuthOutcome.NeedsCode(step) // unknown e-mail: stay neutral
                else signIn.resend(step) { it.sendResetPasswordEmailCode() }
        }
    }

    private suspend fun SignIn?.resend(
        step: CodeChallenge,
        call: suspend (SignIn) -> ClerkResult<SignIn, ClerkErrorResponse>,
    ): AuthOutcome {
        val signIn = this ?: return AuthOutcome.Failed(null, AuthFailureKind.FLOW_EXPIRED)
        return when (val r = call(signIn)) {
            is ClerkResult.Success -> { pendingSignIn = r.value; AuthOutcome.NeedsCode(step) }
            is ClerkResult.Failure -> failure(r)
        }
    }

    // ---- Forgot password ------------------------------------------------------------------------

    /**
     * Starts a password reset: Clerk e-mails a one-time code to the address if an account exists.
     * The result is deliberately the same whether or not the e-mail is registered, so the screen
     * can't be used to find out who has an account.
     */
    suspend fun startPasswordReset(email: String): AuthOutcome = guarded {
        reset()
        forgotPasswordFlow = true
        val step = CodeChallenge(CodePurpose.PASSWORD_RESET, email)
        when (val created = Clerk.auth.signIn { this.email = email }) {
            is ClerkResult.Success -> when (val sent = created.value.sendResetPasswordEmailCode()) {
                is ClerkResult.Success -> {
                    pendingSignIn = sent.value
                    challenge = step
                    AuthOutcome.NeedsCode(step)
                }
                is ClerkResult.Failure -> failureOrNeutral(sent, step)
            }
            is ClerkResult.Failure -> failureOrNeutral(created, step)
        }
    }

    /** "No such account" is reported as success (nothing was sent); anything else is a real problem to show. */
    private fun failureOrNeutral(result: ClerkResult.Failure<ClerkErrorResponse>, step: CodeChallenge): AuthOutcome {
        val kind = AuthErrorClassifier.classify(result.error?.errors?.firstOrNull()?.code, result.code)
        if (kind == AuthFailureKind.INVALID_CREDENTIALS) {
            pendingSignIn = null
            challenge = step
            return AuthOutcome.NeedsCode(step)
        }
        return failure(result)
    }

    /** Sets the new password for a forgot-password flow, or for a sign-in where Clerk demands a change. */
    suspend fun submitNewPassword(newPassword: String): AuthOutcome = guarded {
        val signIn = pendingSignIn ?: return@guarded AuthOutcome.Failed(null, AuthFailureKind.FLOW_EXPIRED)
        when (val r = signIn.resetPassword(newPassword, signOutOfOtherSessions = false)) {
            is ClerkResult.Success -> {
                val updated = r.value
                pendingSignIn = updated
                if (forgotPasswordFlow && updated.status == SignIn.Status.COMPLETE) {
                    // The password changed. Make the user prove it by signing in again with it.
                    discardSession(updated.createdSessionId)
                    reset()
                    AuthOutcome.PasswordResetDone
                } else {
                    handleSignIn(updated)
                }
            }
            is ClerkResult.Failure -> failure(r)
        }
    }

    /** The user backed out of a challenge. Clerk discards the half-finished flow on its own. */
    fun cancelFlow() = reset()

    // ---- Sign out -------------------------------------------------------------------------------

    suspend fun signOut() {
        reset()
        try {
            Clerk.auth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The SDK clears local session state even if the network call fails.
        }
    }

    // ---- Helpers --------------------------------------------------------------------------------

    private fun reset() {
        pendingSignIn = null
        pendingSignUp = null
        challenge = null
        forgotPasswordFlow = false
    }

    /** If a session already exists the user should not be asked to sign in again. */
    private fun alreadySignedIn(): AuthOutcome? = if (isSignedIn) AuthOutcome.SignedIn else null

    private suspend fun discardSession(sessionId: String?) {
        try {
            if (sessionId != null && Clerk.session?.id == sessionId) Clerk.auth.signOut(sessionId) else Clerk.auth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // best effort
        }
    }

    private suspend fun activate(sessionId: String?): AuthOutcome {
        if (Clerk.activeSession != null) return AuthOutcome.SignedIn
        if (sessionId == null) return AuthOutcome.Unsupported("session_missing")
        return when (val r = Clerk.auth.setActive(sessionId = sessionId)) {
            is ClerkResult.Success ->
                if (Clerk.activeSession != null) AuthOutcome.SignedIn else AuthOutcome.Unsupported("session_not_active")
            is ClerkResult.Failure -> failure(r)
        }
    }

    private fun failure(result: ClerkResult.Failure<ClerkErrorResponse>): AuthOutcome {
        if (result.throwable is IOException) return AuthOutcome.Failed(null, AuthFailureKind.NETWORK)
        val first = result.error?.errors?.firstOrNull()
        val code = first?.code
        // Clerk says a session already exists: that is a success for our purposes.
        if (code == "session_exists" && Clerk.activeSession != null) return AuthOutcome.SignedIn
        return AuthOutcome.Failed(first?.longMessage ?: first?.message, AuthErrorClassifier.classify(code, result.code))
    }

    private suspend fun guarded(block: suspend () -> AuthOutcome): AuthOutcome = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        AuthOutcome.Failed(null, AuthFailureKind.NETWORK)
    } catch (e: Exception) {
        AuthOutcome.Failed(null, AuthFailureKind.GENERIC)
    }
}
