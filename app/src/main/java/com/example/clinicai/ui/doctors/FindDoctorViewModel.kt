package com.example.clinicai.ui.doctors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.location.LocationRepository
import com.example.clinicai.data.location.LocationStatus
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.data.model.Specialization
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.data.repository.DoctorRepository
import com.example.clinicai.domain.DoctorFilter
import com.example.clinicai.domain.GeoDistance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FindUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val doctors: List<Doctor> = emptyList(),
    val query: String = "",
    val specializationId: String? = null,
    val location: LocationStatus = LocationStatus.NotRequested,
) {
    val specialties: List<Specialization> get() = DoctorFilter.specialties(doctors)

    /** Distance in km per doctor id, only for doctors whose clinic has real coordinates. */
    val distances: Map<String, Double>
        get() {
            val here = (location as? LocationStatus.Available)?.location ?: return emptyMap()
            return doctors.mapNotNull { d ->
                val clinic = d.clinic?.takeIf { it.hasCoordinates } ?: return@mapNotNull null
                d.id to GeoDistance.kilometres(here.latitude, here.longitude, clinic.latitude, clinic.longitude)
            }.toMap()
        }

    val sortedByDistance: Boolean get() = location is LocationStatus.Available

    /** Doctors matching the search and, when "Near me" is on, only those within [GeoDistance.NEARBY_RADIUS_KM], nearest first. */
    val visible: List<Doctor>
        get() = DoctorFilter.apply(doctors, query, specializationId, distances.takeIf { sortedByDistance }, GeoDistance.NEARBY_RADIUS_KM)

    /** "Near me" is on but no clinic with a known position is within the radius. */
    val nothingNearby: Boolean get() = sortedByDistance && !loading && error == null && distances.values.none { GeoDistance.isWithin(it) }
}

class FindDoctorViewModel(
    private val repository: DoctorRepository,
    private val location: LocationRepository,
    initialSpecializationId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(FindUiState(specializationId = initialSpecializationId, location = location.status.value))
    val state: StateFlow<FindUiState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch { location.status.collect { status -> _state.update { it.copy(location = status) } } }
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.allDoctors()) {
                is ApiResult.Success -> _state.update { current ->
                    // Keep a deep-linked specialty only if it exists among the loaded doctors.
                    val keep = current.specializationId?.takeIf { id -> result.data.any { it.specializationId == id } }
                    current.copy(loading = false, doctors = result.data, specializationId = keep)
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
            }
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    fun selectSpecialty(id: String?) = _state.update { it.copy(specializationId = id) }

    /** Re-takes the position when "Near me" is on and the last fix is old; a fresh one is reused (no continuous tracking). */
    fun refreshLocationIfActive() {
        if (location.status.value is LocationStatus.Available) viewModelScope.launch { location.refresh() }
    }

    /** Stop showing distances and forget the position. */
    fun clearLocation() = location.clear()
}
