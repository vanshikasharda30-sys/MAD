package com.example.clinicai.ui.appointments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.clinicai.R
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.databinding.FragmentMyAppointmentsBinding
import com.example.clinicai.ui.common.StateViewController
import com.example.clinicai.ui.common.UiState
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.authIntent
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.confirmDestructive
import com.example.clinicai.ui.common.openAppointment
import com.example.clinicai.ui.common.show
import com.example.clinicai.ui.common.snack
import com.example.clinicai.ui.common.toMessage

class MyAppointmentsFragment : Fragment() {

    private var _binding: FragmentMyAppointmentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel by appViewModels { AppointmentsViewModel(it.appointments, it.receipts, it.auth) }
    private lateinit var adapter: AppointmentAdapter
    private lateinit var stateView: StateViewController

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.load()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AppointmentAdapter(
            onOpen = { openAppointment(it.id) },
            onCancel = { item ->
                requireContext().confirmDestructive(
                    title = getString(R.string.appts_cancel_confirm_title),
                    message = getString(R.string.appts_cancel_confirm_body),
                    confirmLabel = getString(R.string.appts_cancel),
                    cancelLabel = getString(R.string.appts_cancel_keep),
                ) { viewModel.cancel(item.id) }
            },
            onReview = { item ->
                ReviewDialog.show(requireContext()) { rating, comment -> viewModel.review(item.id, rating, comment) }
            },
        )
        binding.appointmentList.adapter = adapter
        stateView = StateViewController(binding.listState, requireContext()) { viewModel.load() }
        binding.signInButton.setOnClickListener { signInLauncher.launch(authIntent(requireContext())) }
        binding.refresh.setOnRefreshListener { viewModel.load() }

        collectFlow(viewModel.state) { render(it) }
        collectFlow(viewModel.events) { event ->
            when (event) {
                AppointmentsEvent.Cancelled -> binding.root.snack(getString(R.string.appts_cancelled))
                AppointmentsEvent.Reviewed -> binding.root.snack(getString(R.string.review_thanks))
                is AppointmentsEvent.Failed -> binding.root.snack(event.error.toMessage(requireContext()))
                is AppointmentsEvent.InvalidInput -> binding.root.snack(getString(R.string.review_pick_rating))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh when coming back to the tab (e.g. after booking or cancelling elsewhere).
        if (viewModel.state.value !is UiState.Loading) viewModel.load()
    }

    private fun render(state: UiState<AppointmentsContent>) {
        binding.refresh.isRefreshing = false
        val signedOut = state is UiState.Error && state.error is AppError.Unauthorized
        binding.signedOutGroup.show(signedOut)
        binding.refresh.show(!signedOut)
        binding.localOnlyNote.show(state is UiState.Success && state.data.localOnly)

        when (state) {
            UiState.Loading -> stateView.showLoading()
            is UiState.Empty -> stateView.showEmpty(getString(R.string.appts_empty_title), getString(R.string.appts_empty_body))
            is UiState.Error -> if (signedOut) stateView.hide() else stateView.showError(state.error)
            is UiState.Success -> {
                stateView.hide()
                adapter.submitList(state.data.items)
            }
        }
        if (state !is UiState.Success) adapter.submitList(emptyList())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
