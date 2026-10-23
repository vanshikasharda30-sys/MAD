package com.example.clinicai.ui.doctors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.config.AppConstants
import com.example.clinicai.data.location.LocationRepository
import com.example.clinicai.data.location.LocationStatus
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.domain.DistanceResolver
import com.example.clinicai.domain.DoctorDistance
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.repository.DoctorRepository
import com.example.clinicai.domain.BookingDates
import com.example.clinicai.domain.DayItem
import com.example.clinicai.domain.SlotGroup
import com.example.clinicai.domain.SlotGrouper
import com.example.clinicai.ui.common.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DoctorDetailViewModel(
    private val repository: DoctorRepository,
    private val location: LocationRepository,
    private val doctorId: String,
) : ViewModel() {


    private val doctorState = MutableStateFlow<UiState<Doctor>>(UiState.Loading)

    val days: List<DayItem> = BookingDates.next(AppConstants.BOOKING_WINDOW_DAYS)

    val doctor: StateFlow<UiState<Doctor>> = doctorState.asStateFlow()

    /** How far the clinic is from the user, from real coordinates; see [DistanceResolver] for the fallbacks. */
    val distance: StateFlow<DoctorDistance> = combine(doctorState, location.status) { doctor, status ->
        DistanceResolver.resolve((doctor as? UiState.Success)?.data?.clinic, status)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DoctorDistance.Hidden)

    val locationStatus: StateFlow<LocationStatus> get() = location.status

    private val _selectedDate = MutableStateFlow(days.first().iso)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _slots = MutableStateFlow<UiState<List<SlotGroup>>>(UiState.Loading)
    val slots: StateFlow<UiState<List<SlotGroup>>> = _slots.asStateFlow()

    private val _selectedSlot = MutableStateFlow<String?>(null)
    val selectedSlot: StateFlow<String?> = _selectedSlot.asStateFlow()

    private var slotJob: Job? = null

    init {
        loadDoctor()
        loadSlots()
    }

    fun loadDoctor() {
        doctorState.value = UiState.Loading
        viewModelScope.launch {
            doctorState.value = when (val result = repository.doctor(doctorId)) {
                is ApiResult.Success -> UiState.Success(result.data)
                is ApiResult.Failure -> UiState.Error(result.error)
            }
        }
    }

    fun selectDate(iso: String) {
        if (iso == _selectedDate.value && _slots.value is UiState.Success) return
        _selectedDate.value = iso
        loadSlots()
    }

    fun selectSlot(slot: String) {
        _selectedSlot.value = slot
    }

    fun loadSlots() {
        slotJob?.cancel()
        _selectedSlot.value = null
        _slots.value = UiState.Loading
        val date = _selectedDate.value
        slotJob = viewModelScope.launch {
            _slots.value = when (val result = repository.availability(doctorId, date)) {
                is ApiResult.Success ->
                    if (result.data.slots.isEmpty()) UiState.Empty() else UiState.Success(SlotGrouper.group(result.data.slots))
                is ApiResult.Failure -> UiState.Error(result.error)
            }
        }
    }
}
