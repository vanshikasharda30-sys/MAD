package com.example.clinicai.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.auth.AuthState
import com.example.clinicai.data.auth.ClerkAuthManager
import com.example.clinicai.data.model.UserProfile
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.data.repository.UserRepository
import com.example.clinicai.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Who is using the staff area. Only a verified STAFF profile unlocks the screens. */
class StaffSessionViewModel(
    private val users: UserRepository,
    private val auth: ClerkAuthManager,
) : ViewModel() {

    private val _profile = MutableStateFlow<UiState<UserProfile>>(UiState.Loading)
    val profile: StateFlow<UiState<UserProfile>> = _profile.asStateFlow()

    val clinicId: String get() = (_profile.value as? UiState.Success)?.data?.clinicId.orEmpty()
    val isAdmin: Boolean get() = (_profile.value as? UiState.Success)?.data?.isAdmin == true
    val name: String
        get() = (_profile.value as? UiState.Success)?.data?.name?.takeIf { it.isNotBlank() }
            ?: auth.accountInfo()?.displayName.orEmpty()

    init {
        load()
    }

    fun load() {
        _profile.value = UiState.Loading
        viewModelScope.launch {
            if (auth.awaitReady() !is AuthState.SignedIn) {
                _profile.value = UiState.Error(AppError.Unauthorized())
                return@launch
            }
            _profile.value = when (val result = users.me()) {
                is ApiResult.Success ->
                    if (result.data.isStaff && !result.data.clinicId.isNullOrBlank()) {
                        UiState.Success(result.data)
                    } else {
                        UiState.Error(AppError.Forbidden())
                    }
                is ApiResult.Failure -> UiState.Error(result.error)
            }
        }
    }
}
