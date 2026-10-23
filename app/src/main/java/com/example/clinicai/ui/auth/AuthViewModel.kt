package com.example.clinicai.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.auth.AuthOutcome
import com.example.clinicai.data.auth.ClerkAuthManager
import com.example.clinicai.data.auth.CodeChallenge
import com.example.clinicai.data.auth.PasswordPolicy
import com.example.clinicai.data.auth.PasswordPolicyRepository
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One-shot results the auth screens react to. */
sealed interface AuthEvent {
    data class Completed(val isStaff: Boolean) : AuthEvent
    data class NeedsCode(val challenge: CodeChallenge) : AuthEvent
    data object NeedsNewPassword : AuthEvent
    data object PasswordResetDone : AuthEvent
    data object CodeResent : AuthEvent
    /** A failure to show. [outcome] is `Failed` or `Unsupported`. */
    data class Error(val outcome: AuthOutcome) : AuthEvent
}

class AuthViewModel(
    private val auth: ClerkAuthManager,
    private val users: UserRepository,
    private val policies: PasswordPolicyRepository,
) : ViewModel() {

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _challenge = MutableStateFlow<CodeChallenge?>(null)
    /** The code the user is currently being asked for (drives the verification screen's wording). */
    val challenge: StateFlow<CodeChallenge?> = _challenge.asStateFlow()

    private val _policy = MutableStateFlow(PasswordPolicy())
    val policy: StateFlow<PasswordPolicy> = _policy.asStateFlow()

    private val _resendIn = MutableStateFlow(0)
    /** Seconds until "Resend code" is allowed again. */
    val resendIn: StateFlow<Int> = _resendIn.asStateFlow()

    private val _passwordUpdated = MutableStateFlow(false)
    /** True after a successful forgot-password flow, so the sign-in screen can say so once. */
    val passwordUpdated: StateFlow<Boolean> = _passwordUpdated.asStateFlow()

    /** True when Clerk forced a password change during sign-in (as opposed to forgot-password). */
    var passwordChangeIsForced: Boolean = false
        private set

    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 8)
    // A SharedFlow (not a Channel) so the activity and the visible fragment both see every event.
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    private var cooldownJob: Job? = null

    init {
        viewModelScope.launch { _policy.value = policies.policy() }
    }

    fun signIn(email: String, password: String) = step { auth.signIn(email.trim(), password) }

    fun signUp(firstName: String, lastName: String, email: String, password: String) =
        step { auth.signUp(firstName.trim(), lastName.trim(), email.trim(), password) }

    fun startPasswordReset(email: String) = step { auth.startPasswordReset(email.trim()) }

    fun submitCode(code: String) = step { auth.submitCode(code.trim()) }

    fun submitNewPassword(password: String) = step { auth.submitNewPassword(password) }

    fun resendCode() {
        if (_busy.value || _resendIn.value > 0) return
        step(isResend = true) { auth.resendCode() }
    }

    fun consumePasswordUpdatedNotice() {
        _passwordUpdated.value = false
    }

    /** The user backed out of a verification / password step. */
    fun cancelFlow() {
        auth.cancelFlow()
        _challenge.value = null
        cooldownJob?.cancel()
        _resendIn.value = 0
    }

    /** Runs one auth step; ignores taps while a step is already in flight. */
    private fun step(isResend: Boolean = false, call: suspend () -> AuthOutcome) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            val outcome = call()
            val event: AuthEvent = when (outcome) {
                AuthOutcome.SignedIn -> {
                    _challenge.value = null
                    val isStaff = (users.me() as? ApiResult.Success)?.data?.isStaff == true
                    AuthEvent.Completed(isStaff)
                }
                is AuthOutcome.NeedsCode -> {
                    _challenge.value = outcome.challenge
                    if (outcome.challenge.canResend) startCooldown()
                    if (isResend) AuthEvent.CodeResent else AuthEvent.NeedsCode(outcome.challenge)
                }
                AuthOutcome.NeedsNewPassword -> {
                    passwordChangeIsForced = _challenge.value?.purpose != com.example.clinicai.data.auth.CodePurpose.PASSWORD_RESET
                    AuthEvent.NeedsNewPassword
                }
                AuthOutcome.PasswordResetDone -> {
                    _challenge.value = null
                    _passwordUpdated.value = true
                    AuthEvent.PasswordResetDone
                }
                else -> AuthEvent.Error(outcome)
            }
            _busy.value = false
            _events.emit(event)
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _resendIn.value = RESEND_COOLDOWN_SECONDS
            while (_resendIn.value > 0) {
                delay(1_000)
                _resendIn.value -= 1
            }
        }
    }

    companion object {
        const val RESEND_COOLDOWN_SECONDS = 30
    }
}
