package com.example.clinicai.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.clinicai.BuildConfig
import com.example.clinicai.R
import com.example.clinicai.config.ApiConfig
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.databinding.FragmentProfileBinding
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.authIntent
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.confirmDestructive
import com.example.clinicai.ui.common.openStaffDashboard
import com.example.clinicai.ui.common.show
import com.example.clinicai.ui.common.toMessage

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel by appViewModels { ProfileViewModel(it.auth, it.users, it.prefs) }

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refresh()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signInButton.setOnClickListener { signInLauncher.launch(authIntent(requireContext())) }
        binding.appointmentsButton.setOnClickListener { findNavController().navigate(R.id.myAppointmentsFragment) }
        binding.staffButton.setOnClickListener { openStaffDashboard() }
        binding.signOutButton.setOnClickListener {
            requireContext().confirmDestructive(
                title = getString(R.string.action_sign_out),
                message = getString(R.string.profile_sign_out_confirm),
                confirmLabel = getString(R.string.action_sign_out),
                cancelLabel = getString(R.string.action_cancel),
            ) { viewModel.signOut() }
        }
        binding.versionText.text = getString(
            R.string.profile_version,
            BuildConfig.VERSION_NAME,
            ApiConfig.environment.name.lowercase(),
        )

        collectFlow(viewModel.state) { state ->
            binding.signedOutGroup.show(!state.signedIn && !state.loading)
            binding.signedInGroup.show(state.signedIn)
            if (!state.signedIn) return@collectFlow

            val profile = state.profile
            val name = profile?.name?.takeIf { it.isNotBlank() } ?: state.account?.displayName.orEmpty()
            binding.accountName.text = name
            binding.accountEmail.text = profile?.email ?: state.account?.email.orEmpty()
            binding.roleChip.show(profile != null)
            binding.roleChip.text = when {
                profile == null -> ""
                profile.isAdmin -> getString(R.string.profile_role_staff_admin)
                profile.isStaff -> getString(R.string.profile_role_staff_reception)
                else -> getString(R.string.profile_role_patient)
            }
            binding.staffButton.show(profile?.isStaff == true)
            binding.profileProgress.show(state.loading)

            val problem = state.profileError
            binding.profileError.show(problem != null)
            if (problem != null) {
                binding.profileError.text = problem.toMessage(requireContext())
            }
            // Signed in with Clerk but the session was rejected: let the user sign out and retry.
            if (problem is AppError.Unauthorized) binding.profileError.show(true)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
