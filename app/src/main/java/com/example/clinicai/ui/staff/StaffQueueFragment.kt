package com.example.clinicai.ui.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.clinicai.R
import com.example.clinicai.config.AppConstants
import com.example.clinicai.databinding.FragmentStaffQueueBinding
import com.example.clinicai.ui.common.StateViewController
import com.example.clinicai.ui.common.UiState
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.pollWhileStarted
import com.example.clinicai.ui.common.sharedAppViewModels
import com.example.clinicai.ui.common.show
import com.example.clinicai.ui.common.snack
import com.example.clinicai.ui.common.toMessage
import com.google.android.material.chip.Chip

/** Live walk-in queue per doctor with Call / Skip / Complete, refreshed every few seconds. */
class StaffQueueFragment : Fragment() {

    private var _binding: FragmentStaffQueueBinding? = null
    private val binding get() = _binding!!

    private val session by sharedAppViewModels { StaffSessionViewModel(it.users, it.auth) }
    private val viewModel by appViewModels { StaffQueueViewModel(it.staff, it.queue, session.clinicId) }
    private lateinit var adapter: QueueEntryAdapter
    private lateinit var stateView: StateViewController
    private var chipsFor: List<String>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStaffQueueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = QueueEntryAdapter { entry, action -> viewModel.perform(entry.queueEntryId, action) }
        binding.queueList.adapter = adapter
        stateView = StateViewController(binding.queueState, requireContext()) { viewModel.loadDoctors() }

        pollWhileStarted(AppConstants.STAFF_QUEUE_POLL_INTERVAL_MS) { viewModel.refresh() }

        collectFlow(viewModel.doctors) { state ->
            binding.queueContent.show(state is UiState.Success)
            when (state) {
                UiState.Loading -> stateView.showLoading()
                is UiState.Error -> stateView.showError(state.error)
                is UiState.Empty -> stateView.showEmpty(getString(R.string.find_empty_title))
                is UiState.Success -> {
                    stateView.hide()
                    val ids = state.data.map { it.id }
                    if (ids != chipsFor) {
                        chipsFor = ids
                        binding.doctorChips.removeAllViews()
                        state.data.forEach { doctor ->
                            val chip = layoutInflater.inflate(R.layout.view_filter_chip, binding.doctorChips, false) as Chip
                            chip.text = doctor.name
                            chip.tag = doctor.id
                            chip.setOnClickListener { viewModel.select(doctor.id) }
                            binding.doctorChips.addView(chip)
                        }
                    }
                }
            }
        }
        collectFlow(viewModel.selectedDoctorId) { selected ->
            for (i in 0 until binding.doctorChips.childCount) {
                val chip = binding.doctorChips.getChildAt(i) as Chip
                chip.isChecked = chip.tag == selected
            }
        }
        collectFlow(viewModel.status) { status ->
            val serving = status?.currentlyServing
            binding.servingValue.text = serving?.let { "#${it.queueNumber}" + (it.patientName?.let { n -> " — $n" } ?: "") }
                ?: getString(R.string.queue_nobody_serving)
            val waiting = status?.waiting.orEmpty()
            adapter.submitList(waiting)
            binding.nobodyWaiting.show(status != null && waiting.isEmpty())
        }
        collectFlow(viewModel.pendingAction) { adapter.busy = it != null }
        collectFlow(viewModel.errors) { binding.root.snack(it.toMessage(requireContext())) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
