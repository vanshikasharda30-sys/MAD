package com.example.clinicai.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.auth.AccountInfo
import com.example.clinicai.data.auth.AuthState
import com.example.clinicai.data.auth.ClerkAuthManager
import com.example.clinicai.data.local.SessionPreferences
import com.example.clinicai.data.model.UserProfile
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val signedIn: Boolean = false,
    val account: AccountInfo? = null,
    val profile: UserProfile? = null,
    val profileError: AppError? = null,
    val loading: Boolean = false,
)

class ProfileViewModel(
    private val auth: ClerkAuthManager,
    private val users: UserRepository,
    private val prefs: SessionPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { auth.authState.collect { refresh() } }
    }

    fun refresh() {
        if (auth.authState.value !is AuthState.SignedIn) {
            _state.value = ProfileUiState(signedIn = false, loading = auth.authState.value is AuthState.Initializing)
            return
        }
        _state.value = ProfileUiState(signedIn = true, account = auth.accountInfo(), loading = true)
        viewModelScope.launch {
            _state.value = when (val result = users.me()) {
                is ApiResult.Success -> _state.value.copy(profile = result.data, profileError = null, loading = false)
                is ApiResult.Failure -> _state.value.copy(profileError = result.error, loading = false)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            prefs.clearUserState()
            refresh()
        }
    }
}
