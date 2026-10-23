package com.example.clinicai.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.model.Appointment
import com.example.clinicai.data.model.DashboardOverview
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.data.repository.StaffRepository
import com.example.clinicai.ui.common.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The website's All / Scheduled / Walk-in tabs on the upcoming-appointments table. */
enum class AppointmentTab { ALL, SCHEDULED, WALK_IN }

data class OverviewContent(
    val overview: DashboardOverview,
    val upcoming: List<Appointment>,
    /** Why the upcoming-appointments list couldn't be loaded, or null if it was. The rest of the overview is still real. */
    val upcomingError: AppError? = null,
    val query: String = "",
    val tab: AppointmentTab = AppointmentTab.ALL,
) {
    val upcomingAvailable: Boolean get() = upcomingError == null

    fun count(tab: AppointmentTab): Int = when (tab) {
        AppointmentTab.ALL -> upcoming.size
        AppointmentTab.SCHEDULED -> upcoming.count { it.isScheduled }
        AppointmentTab.WALK_IN -> upcoming.count { it.isWalkIn }
    }

    val visibleUpcoming: List<Appointment>
        get() {
            val q = query.trim().lowercase()
            return upcoming.filter { a ->
                val matchesTab = when (tab) {
                    AppointmentTab.ALL -> true
                    AppointmentTab.SCHEDULED -> a.isScheduled
                    AppointmentTab.WALK_IN -> a.isWalkIn
                }
                matchesTab && (q.isEmpty() ||
                    a.patient?.name.orEmpty().lowercase().contains(q) ||
                    a.doctor?.name.orEmpty().lowercase().contains(q))
            }
        }
}

class StaffOverviewViewModel(
    private val staff: StaffRepository,
    private val clinicId: String,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<OverviewContent>>(UiState.Loading)
    val state: StateFlow<UiState<OverviewContent>> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    /** Loads (or reloads) the overview. A reload requested while one is already running is ignored. */
    fun load() {
        if (loadJob?.isActive == true) return
        _state.value = UiState.Loading
        loadJob = viewModelScope.launch {
            val overview = async { staff.overview(clinicId) }
            val upcoming = async { staff.upcomingAppointments() }
            _state.value = when (val o = overview.await()) {
                is ApiResult.Failure -> UiState.Error(o.error)
                is ApiResult.Success -> {
                    val u = upcoming.await()
                    UiState.Success(
                        OverviewContent(
                            overview = o.data,
                            upcoming = (u as? ApiResult.Success)?.data.orEmpty(),
                            upcomingError = (u as? ApiResult.Failure)?.error,
                        ),
                    )
                }
            }
        }
    }

    fun setQuery(query: String) = update { it.copy(query = query) }

    fun setTab(tab: AppointmentTab) = update { it.copy(tab = tab) }

    private fun update(change: (OverviewContent) -> OverviewContent) {
        _state.update { current -> if (current is UiState.Success) UiState.Success(change(current.data)) else current }
    }
}
