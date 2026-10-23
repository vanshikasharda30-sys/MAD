package com.example.clinicai.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.model.Appointment
import com.example.clinicai.data.model.DashboardOverview
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.data.repository.AppointmentScope
import com.example.clinicai.data.repository.StaffRepository
import com.example.clinicai.ui.common.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The website's All / Scheduled / Walk-in tabs on the appointments table. */
enum class AppointmentTab { ALL, SCHEDULED, WALK_IN }

data class OverviewContent(
    val overview: DashboardOverview,
    val scope: AppointmentScope = AppointmentScope.UPCOMING,
    val appointments: List<Appointment> = emptyList(),
    /** How many appointments the server says match [scope]; larger than [appointments] when it capped the list. */
    val total: Int = appointments.size,
    /** Why the appointments couldn't be loaded, or null if they were. A failure is never shown as "no appointments". */
    val appointmentsError: AppError? = null,
    /** The appointments are being (re)loaded while the rest of the overview stays on screen. */
    val appointmentsLoading: Boolean = false,
    val query: String = "",
    val tab: AppointmentTab = AppointmentTab.ALL,
    val loadedAtMillis: Long = 0L,
) {
    val appointmentsAvailable: Boolean get() = appointmentsError == null

    fun count(tab: AppointmentTab): Int = when (tab) {
        AppointmentTab.ALL -> appointments.size
        AppointmentTab.SCHEDULED -> appointments.count { it.isScheduled }
        AppointmentTab.WALK_IN -> appointments.count { it.isWalkIn }
    }

    val visibleAppointments: List<Appointment>
        get() {
            val q = query.trim().lowercase()
            return appointments.filter { a ->
                val matchesTab = when (tab) {
                    AppointmentTab.ALL -> true
                    AppointmentTab.SCHEDULED -> a.isScheduled
                    AppointmentTab.WALK_IN -> a.isWalkIn
                }
                matchesTab && (q.isEmpty() ||
                    a.patient?.name.orEmpty().lowercase().contains(q) ||
                    a.doctor?.name.orEmpty().lowercase().contains(q) ||
                    a.id.lowercase().contains(q))
            }
        }
}

class StaffOverviewViewModel(
    private val staff: StaffRepository,
    private val clinicId: String,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<OverviewContent>>(UiState.Loading)
    val state: StateFlow<UiState<OverviewContent>> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var appointmentsJob: Job? = null

    init {
        load()
    }

    /** Loads (or reloads) the whole overview. A reload requested while one is already running is ignored. */
    fun load() {
        if (loadJob?.isActive == true) return
        appointmentsJob?.cancel()
        val scope = (_state.value as? UiState.Success)?.data?.scope ?: AppointmentScope.UPCOMING
        _state.value = UiState.Loading
        loadJob = viewModelScope.launch {
            val overview = async { staff.overview(clinicId) }
            val appointments = async { staff.appointments(scope) }
            _state.value = when (val o = overview.await()) {
                is ApiResult.Failure -> UiState.Error(o.error)
                is ApiResult.Success -> {
                    val content = OverviewContent(overview = o.data, scope = scope, loadedAtMillis = clock())
                    UiState.Success(content.withAppointments(appointments.await()))
                }
            }
        }
    }

    /** Switches between Today / Upcoming / Past; the latest choice wins if the user taps quickly. */
    fun setScope(scope: AppointmentScope) {
        val current = (_state.value as? UiState.Success)?.data ?: return
        if (current.scope == scope && !current.appointmentsLoading && current.appointmentsAvailable) return
        update { it.copy(scope = scope, appointmentsLoading = true, appointmentsError = null) }
        reloadAppointments(scope, force = true)
    }

    /** Re-fetches just the appointment list (e.g. a patient has booked since); the rest of the screen stays put. */
    fun refreshAppointments() {
        val current = (_state.value as? UiState.Success)?.data ?: return
        update { it.copy(appointmentsLoading = true) }
        reloadAppointments(current.scope, force = true)
    }

    /** Called when the screen comes back into view: refresh quietly if the data is older than [maxAgeMillis]. */
    fun refreshIfStale(maxAgeMillis: Long = STALE_AFTER_MILLIS) {
        val current = (_state.value as? UiState.Success)?.data ?: return
        if (clock() - current.loadedAtMillis < maxAgeMillis) return
        if (appointmentsJob?.isActive == true || loadJob?.isActive == true) return
        reloadAppointments(current.scope, force = false)
    }

    private fun reloadAppointments(scope: AppointmentScope, force: Boolean) {
        if (!force && appointmentsJob?.isActive == true) return
        appointmentsJob?.cancel()
        appointmentsJob = viewModelScope.launch {
            val result = staff.appointments(scope)
            update { current -> if (current.scope == scope) current.withAppointments(result).copy(loadedAtMillis = clock()) else current }
        }
    }

    fun setQuery(query: String) = update { it.copy(query = query) }

    fun setTab(tab: AppointmentTab) = update { it.copy(tab = tab) }

    private fun OverviewContent.withAppointments(result: ApiResult<com.example.clinicai.data.repository.AppointmentPage>): OverviewContent =
        when (result) {
            // A failure yields the error and no list at all, so it can never read as "no appointments".
            is ApiResult.Failure -> copy(appointments = emptyList(), total = 0, appointmentsError = result.error, appointmentsLoading = false)
            is ApiResult.Success -> copy(appointments = result.data.items, total = result.data.total, appointmentsError = null, appointmentsLoading = false)
        }

    private fun update(change: (OverviewContent) -> OverviewContent) {
        _state.update { current -> if (current is UiState.Success) UiState.Success(change(current.data)) else current }
    }

    private companion object {
        const val STALE_AFTER_MILLIS = 30_000L
    }
}
