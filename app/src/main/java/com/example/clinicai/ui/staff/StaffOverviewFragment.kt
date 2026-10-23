package com.example.clinicai.ui.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.example.clinicai.R
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.databinding.FragmentStaffOverviewBinding
import com.example.clinicai.ui.common.toMessage
import com.example.clinicai.ui.common.StateViewController
import com.example.clinicai.ui.common.UiState
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.sharedAppViewModels
import com.example.clinicai.ui.common.show
import java.util.Calendar

class StaffOverviewFragment : Fragment() {

    private var _binding: FragmentStaffOverviewBinding? = null
    private val binding get() = _binding!!

    private val session by sharedAppViewModels { StaffSessionViewModel(it.users, it.auth) }
    private val viewModel by appViewModels { StaffOverviewViewModel(it.staff, session.clinicId) }
    private val adapter = StaffAppointmentAdapter()
    private lateinit var stateView: StateViewController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStaffOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appointmentList.adapter = adapter
        stateView = StateViewController(binding.overviewState, requireContext()) { viewModel.load() }
        binding.searchInput.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
        binding.refresh.setOnRefreshListener { viewModel.load() }
        binding.upcomingRetry.setOnClickListener { viewModel.load() }
        binding.tabAll.setOnClickListener { viewModel.setTab(AppointmentTab.ALL) }
        binding.tabScheduled.setOnClickListener { viewModel.setTab(AppointmentTab.SCHEDULED) }
        binding.tabWalkIn.setOnClickListener { viewModel.setTab(AppointmentTab.WALK_IN) }

        collectFlow(viewModel.state) { state ->
            binding.refresh.isRefreshing = false
            binding.content.show(state is UiState.Success)
            when (state) {
                UiState.Loading -> stateView.showLoading()
                is UiState.Error -> stateView.showError(state.error)
                is UiState.Empty -> stateView.hide()
                is UiState.Success -> { stateView.hide(); bind(state.data) }
            }
        }
    }

    private fun bind(content: OverviewContent) {
        val overview = content.overview
        binding.clinicName.text = overview.clinic?.name.orEmpty()
        binding.clinicLocality.text = listOfNotNull(overview.clinic?.area, overview.clinic?.city)
            .filter { it.isNotBlank() }.joinToString(", ")

        val first = session.name.substringBefore(" ").trim()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = getString(
            when {
                hour < 12 -> R.string.staff_greeting_morning
                hour < 17 -> R.string.staff_greeting_afternoon
                else -> R.string.staff_greeting_evening
            },
        )
        binding.greeting.text = if (first.isEmpty()) greeting else "$greeting, $first"

        binding.activeDoctorsCount.text = overview.activeDoctorCount.toString()
        binding.rosterNote.text = getString(R.string.staff_on_roster, overview.totalDoctorCount)
        binding.rosterNames.text = overview.activeDoctors.joinToString("\n") { d ->
            d.name + (d.specializationName?.let { " · $it" } ?: "")
        }
        binding.rosterNames.show(overview.activeDoctors.isNotEmpty())

        binding.queueCount.text = overview.queueWaiting.toString()
        binding.queueNote.text = when {
            overview.queueWaiting == 0 -> getString(R.string.staff_queue_clear)
            overview.avgWaitMinutes != null -> getString(R.string.staff_avg_wait, overview.avgWaitMinutes)
            else -> ""
        }

        binding.tabAll.text = getString(R.string.staff_tab_all, content.count(AppointmentTab.ALL))
        binding.tabScheduled.text = getString(R.string.staff_tab_scheduled, content.count(AppointmentTab.SCHEDULED))
        binding.tabWalkIn.text = getString(R.string.staff_tab_walk_in, content.count(AppointmentTab.WALK_IN))
        binding.tabAll.isChecked = content.tab == AppointmentTab.ALL
        binding.tabScheduled.isChecked = content.tab == AppointmentTab.SCHEDULED
        binding.tabWalkIn.isChecked = content.tab == AppointmentTab.WALK_IN

        // The rest of the overview is real even when only this list is unavailable; say exactly why.
        val visible = content.visibleUpcoming
        adapter.submitList(visible)
        val error = content.upcomingError
        binding.upcomingFilters.show(error == null)
        binding.upcomingEmpty.show(error != null || visible.isEmpty())
        binding.upcomingRetry.show(error != null && error !is AppError.EndpointMissing)
        binding.upcomingEmpty.text = when {
            error is AppError.EndpointMissing -> getString(R.string.staff_upcoming_not_available)
            error != null -> error.toMessage(requireContext())
            content.upcoming.isEmpty() -> getString(R.string.staff_upcoming_empty)
            else -> getString(R.string.staff_upcoming_no_match)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
