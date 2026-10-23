package com.example.clinicai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.auth.AuthState
import com.example.clinicai.data.auth.ClerkAuthManager
import com.example.clinicai.data.model.Clinic
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.repository.DoctorRepository
import com.example.clinicai.data.repository.UserRepository
import com.example.clinicai.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** How many clinics the home screen shows before "See more". */
const val INITIAL_CLINIC_COUNT = 5

class HomeViewModel(
    private val doctors: DoctorRepository,
    private val users: UserRepository,
    private val auth: ClerkAuthManager,
) : ViewModel() {

    private val _clinics = MutableStateFlow<UiState<List<Clinic>>>(UiState.Loading)
    val clinics: StateFlow<UiState<List<Clinic>>> = _clinics.asStateFlow()

    private val _expanded = MutableStateFlow(false)
    /** Whether the partner-clinic list shows every clinic or just the first few. */
    val expanded: StateFlow<Boolean> = _expanded.asStateFlow()

    fun toggleExpanded() {
        _expanded.value = !_expanded.value
    }

    private val _isStaff = MutableStateFlow(false)
    val isStaff: StateFlow<Boolean> = _isStaff.asStateFlow()

    init {
        loadClinics()
        // The staff shortcut follows the sign-in state, including a session restored after launch.
        viewModelScope.launch { auth.authState.collect { refreshRole() } }
    }

    fun loadClinics() {
        _clinics.value = UiState.Loading
        viewModelScope.launch {
            _clinics.value = when (val result = doctors.partnerClinics()) {
                is ApiResult.Success ->
                    if (result.data.isEmpty()) UiState.Empty() else UiState.Success(result.data)
                is ApiResult.Failure -> UiState.Error(result.error)
            }
        }
    }

    /** Staff accounts get a shortcut to the dashboard, like the website's header link. */
    fun refreshRole() {
        if (auth.authState.value !is AuthState.SignedIn) {
            _isStaff.value = false
            return
        }
        viewModelScope.launch {
            _isStaff.value = (users.me() as? ApiResult.Success)?.data?.isStaff == true
        }
    }
}
