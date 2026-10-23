package com.example.clinicai.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.clinicai.R
import com.example.clinicai.databinding.FragmentSignUpBinding
import com.example.clinicai.ui.common.clearErrorOnChange
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.hideKeyboard
import com.example.clinicai.ui.common.setErrorIf
import com.example.clinicai.ui.common.show
import com.example.clinicai.ui.common.text
import com.example.clinicai.util.Validators

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel get() = (requireActivity() as AuthActivity).viewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.firstNameInput.clearErrorOnChange(binding.firstNameLayout)
        binding.lastNameInput.clearErrorOnChange(binding.lastNameLayout)
        binding.emailInput.clearErrorOnChange(binding.emailLayout)
        binding.passwordInput.clearErrorOnChange(binding.passwordLayout)

        binding.createButton.setOnClickListener { submit() }
        binding.haveAccountLink.setOnClickListener { findNavController().navigateUp() }

        collectFlow(viewModel.busy) { busy ->
            binding.createButton.isEnabled = !busy
            binding.progress.show(busy)
        }
        // The password rules come from the real Clerk configuration (e.g. 15+ characters).
        collectFlow(viewModel.policy) { policy ->
            binding.passwordLayout.helperText = policy.hintText(requireContext())
        }
        collectFlow(viewModel.events) { event ->
            when (event) {
                is AuthEvent.NeedsCode -> findNavController().navigate(R.id.verifyCodeFragment)
                is AuthEvent.Error -> {
                    binding.errorText.text = event.outcome.toMessage(requireContext())
                    binding.errorText.show(true)
                }
                else -> Unit
            }
        }
    }

    private fun submit() {
        binding.root.hideKeyboard()
        binding.errorText.show(false)
        val first = binding.firstNameLayout.text.trim()
        val last = binding.lastNameLayout.text.trim()
        val email = binding.emailLayout.text.trim()
        val password = binding.passwordLayout.text
        var valid = true
        if (!Validators.isNotBlank(first)) { binding.firstNameLayout.setErrorIf(getString(R.string.auth_error_name)); valid = false }
        if (!Validators.isNotBlank(last)) { binding.lastNameLayout.setErrorIf(getString(R.string.auth_error_name)); valid = false }
        if (!Validators.isValidEmail(email)) { binding.emailLayout.setErrorIf(getString(R.string.auth_error_email)); valid = false }
        viewModel.policy.value.errorFor(requireContext(), password)?.let { binding.passwordLayout.setErrorIf(it); valid = false }
        if (valid) viewModel.signUp(first, last, email, password)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
