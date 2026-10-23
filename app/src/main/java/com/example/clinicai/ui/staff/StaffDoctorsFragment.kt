package com.example.clinicai.ui.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.clinicai.R
import com.example.clinicai.databinding.FragmentStaffDoctorsBinding
import com.example.clinicai.ui.common.StateViewController
import com.example.clinicai.ui.common.UiState
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.confirmDestructive
import com.example.clinicai.ui.common.sharedAppViewModels
import com.example.clinicai.ui.common.show
import com.example.clinicai.ui.common.snack
import com.example.clinicai.ui.common.toMessage

class StaffDoctorsFragment : Fragment() {

    private var _binding: FragmentStaffDoctorsBinding? = null
    private val binding get() = _binding!!

    private val session by sharedAppViewModels { StaffSessionViewModel(it.users, it.auth) }
    private val viewModel by appViewModels { StaffDoctorsViewModel(it.staff, it.doctors, session.clinicId) }
    private lateinit var stateView: StateViewController
    private lateinit var adapter: StaffDoctorAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStaffDoctorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = StaffDoctorAdapter(canDeactivate = session.isAdmin) { doctor ->
            requireContext().confirmDestructive(
                title = getString(R.string.staff_deactivate),
                message = getString(R.string.staff_deactivate_confirm),
                confirmLabel = getString(R.string.staff_deactivate),
                cancelLabel = getString(R.string.action_cancel),
            ) { viewModel.deactivate(doctor.id) }
        }
        binding.doctorList.adapter = adapter
        stateView = StateViewController(binding.doctorsState, requireContext()) { viewModel.load() }

        binding.addDoctorButton.show(session.isAdmin)
        binding.addDoctorButton.setOnClickListener {
            AddDoctorDialog.show(requireContext(), viewModel.specializations.value) { viewModel.add(it) }
        }

        collectFlow(viewModel.state) { state ->
            binding.doctorList.show(state is UiState.Success)
            when (state) {
                UiState.Loading -> stateView.showLoading()
                is UiState.Error -> stateView.showError(state.error)
                is UiState.Empty -> stateView.showEmpty(getString(R.string.find_empty_title))
                is UiState.Success -> {
                    stateView.hide()
                    adapter.submitList(state.data)
                    binding.activeCount.text = getString(R.string.staff_doctors_active_count, state.data.size)
                }
            }
        }
        collectFlow(viewModel.busy) { binding.addDoctorButton.isEnabled = !it }
        collectFlow(viewModel.events) { event ->
            when (event) {
                DoctorsEvent.Added -> binding.root.snack(getString(R.string.staff_doctor_added))
                DoctorsEvent.Deactivated -> binding.root.snack(getString(R.string.staff_deactivated))
                is DoctorsEvent.Failed -> binding.root.snack(event.error.toMessage(requireContext()))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
