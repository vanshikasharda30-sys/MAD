package com.example.clinicai.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.clinicai.R
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.databinding.FragmentHomeBinding
import com.example.clinicai.ui.common.StateViewController
import com.example.clinicai.ui.common.UiState
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.openCheckin
import com.example.clinicai.ui.common.openFindDoctors
import com.example.clinicai.ui.common.openStaffDashboard
import com.example.clinicai.ui.common.show
import kotlinx.coroutines.flow.combine

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel by appViewModels { HomeViewModel(it.doctors, it.users, it.auth) }
    private lateinit var stateView: StateViewController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = PartnerClinicAdapter { clinic -> openCheckin(clinic.qrToken) }
        binding.partnerList.adapter = adapter
        stateView = StateViewController(binding.partnerState, requireContext()) { viewModel.loadClinics() }

        binding.actionFindDoctor.setOnClickListener { openFindDoctors() }
        binding.heroFind.setOnClickListener { openFindDoctors() }
        binding.heroAsk.setOnClickListener { findNavController().navigate(R.id.chatFragment) }
        binding.actionAsk.setOnClickListener { findNavController().navigate(R.id.chatFragment) }
        binding.actionWalkIn.setOnClickListener { openCheckin() }
        binding.actionAppointments.setOnClickListener { findNavController().navigate(R.id.myAppointmentsFragment) }
        binding.actionFaq.setOnClickListener { findNavController().navigate(R.id.faqFragment) }
        binding.staffDashboardButton.setOnClickListener { openStaffDashboard() }

        binding.seeMoreButton.setOnClickListener { viewModel.toggleExpanded() }

        collectFlow(combine(viewModel.clinics, viewModel.expanded) { clinics, expanded -> clinics to expanded }) { (state, expanded) ->
            when (state) {
                UiState.Loading -> stateView.showLoading()
                is UiState.Empty -> stateView.showEmpty(getString(R.string.home_partner_empty))
                is UiState.Error -> stateView.showError(state.error)
                is UiState.Success -> {
                    stateView.hide()
                    val all = state.data
                    adapter.submitList(if (expanded) all else all.take(INITIAL_CLINIC_COUNT))
                    val hidden = all.size - INITIAL_CLINIC_COUNT
                    binding.seeMoreButton.show(hidden > 0)
                    binding.seeMoreButton.text =
                        if (expanded) getString(R.string.home_show_less) else getString(R.string.home_see_more_count, hidden)
                    binding.seeMoreButton.setIconResource(if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
                }
            }
            binding.partnerList.show(state is UiState.Success)
            if (state !is UiState.Success) binding.seeMoreButton.show(false)
        }
        collectFlow(viewModel.isStaff) { binding.staffDashboardButton.show(it) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshRole()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
