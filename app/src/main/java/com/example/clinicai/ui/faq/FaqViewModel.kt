package com.example.clinicai.ui.faq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.config.AppConstants
import com.example.clinicai.data.model.Clinic
import com.example.clinicai.data.model.Faq
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.repository.ClinicRepository
import com.example.clinicai.data.repository.DoctorRepository
import com.example.clinicai.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FaqScreen(val clinics: List<Clinic>, val selected: Clinic?, val faqs: List<Faq>)

class FaqViewModel(
    private val clinics: ClinicRepository,
    private val doctors: DoctorRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<FaqScreen>>(UiState.Loading)
    val state: StateFlow<UiState<FaqScreen>> = _state.asStateFlow()

    private var clinicList: List<Clinic> = emptyList()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            when (val result = doctors.partnerClinics()) {
                is ApiResult.Success -> {
                    clinicList = result.data
                    val initial = clinicList.firstOrNull { it.id == AppConstants.DEFAULT_CLINIC_ID } ?: clinicList.firstOrNull()
                    if (initial == null) _state.value = UiState.Empty() else select(initial)
                }
                is ApiResult.Failure -> _state.value = UiState.Error(result.error)
            }
        }
    }

    fun select(clinic: Clinic) {
        viewModelScope.launch {
            _state.value = when (val result = clinics.faqs(clinic.id)) {
                is ApiResult.Success -> UiState.Success(FaqScreen(clinicList, clinic, result.data))
                is ApiResult.Failure -> UiState.Error(result.error)
            }
        }
    }
}
