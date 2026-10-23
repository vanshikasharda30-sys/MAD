package com.example.clinicai.ui.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.clinicai.R
import com.example.clinicai.databinding.FragmentStaffMembersBinding
import com.example.clinicai.ui.common.StateViewController
import com.example.clinicai.ui.common.UiState
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.clearErrorOnChange
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.hideKeyboard
import com.example.clinicai.ui.common.setErrorIf
import com.example.clinicai.ui.common.sharedAppViewModels
import com.example.clinicai.ui.common.show
import com.example.clinicai.ui.common.snack
import com.example.clinicai.ui.common.text
import com.example.clinicai.ui.common.toMessage
import com.example.clinicai.util.Validators

class StaffMembersFragment : Fragment() {

    private var _binding: FragmentStaffMembersBinding? = null
    private val binding get() = _binding!!

    private val session by sharedAppViewModels { StaffSessionViewModel(it.users, it.auth) }
    private val viewModel by appViewModels { StaffMembersViewModel(it.staff) }
    private val adapter = StaffMemberAdapter()
    private lateinit var stateView: StateViewController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStaffMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.memberList.adapter = adapter
        stateView = StateViewController(binding.membersState, requireContext()) { viewModel.load() }

        binding.inviteCard.show(session.isAdmin)
        binding.inviteNameInput.clearErrorOnChange(binding.inviteNameLayout)
        binding.inviteEmailInput.clearErrorOnChange(binding.inviteEmailLayout)
        binding.inviteButton.setOnClickListener { submitInvite() }

        collectFlow(viewModel.state) { state ->
            binding.memberList.show(state is UiState.Success)
            when (state) {
                UiState.Loading -> stateView.showLoading()
                is UiState.Error -> stateView.showError(state.error)
                is UiState.Empty -> stateView.showEmpty(getString(R.string.find_empty_title))
                is UiState.Success -> { stateView.hide(); adapter.submitList(state.data) }
            }
        }
        collectFlow(viewModel.busy) { binding.inviteButton.isEnabled = !it }
        collectFlow(viewModel.events) { event ->
            when (event) {
                MembersEvent.Invited -> {
                    binding.inviteNameInput.setText("")
                    binding.inviteEmailInput.setText("")
                    binding.root.snack(getString(R.string.members_invited))
                }
                is MembersEvent.Failed -> binding.root.snack(event.error.toMessage(requireContext()))
            }
        }
    }

    private fun submitInvite() {
        binding.root.hideKeyboard()
        val name = binding.inviteNameLayout.text.trim()
        val email = binding.inviteEmailLayout.text.trim()
        var valid = true
        if (name.isEmpty()) { binding.inviteNameLayout.setErrorIf(getString(R.string.validation_required)); valid = false }
        if (!Validators.isValidEmail(email)) { binding.inviteEmailLayout.setErrorIf(getString(R.string.validation_email)); valid = false }
        if (!valid) return
        val role = if (binding.roleAdmin.isChecked) "ADMIN" else "RECEPTION"
        viewModel.invite(name, email, role)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
