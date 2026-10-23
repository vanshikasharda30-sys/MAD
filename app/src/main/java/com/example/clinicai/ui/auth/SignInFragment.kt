package com.example.clinicai.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.clinicai.R
import com.example.clinicai.databinding.FragmentSignInBinding
import com.example.clinicai.ui.common.clearErrorOnChange
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.hideKeyboard
import com.example.clinicai.ui.common.setErrorIf
import com.example.clinicai.ui.common.show
import com.example.clinicai.ui.common.text
import com.example.clinicai.util.Validators

class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel get() = (requireActivity() as AuthActivity).viewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.emailInput.clearErrorOnChange(binding.emailLayout)
        binding.passwordInput.clearErrorOnChange(binding.passwordLayout)

        binding.signInButton.setOnClickListener { submit() }
        binding.createAccountLink.setOnClickListener { findNavController().navigate(R.id.signUpFragment) }
        binding.forgotPasswordLink.setOnClickListener {
            binding.root.hideKeyboard()
            findNavController().navigate(R.id.forgotPasswordFragment)
        }

        collectFlow(viewModel.busy) { busy ->
            binding.signInButton.isEnabled = !busy
            binding.progress.show(busy)
        }
        collectFlow(viewModel.passwordUpdated) { updated ->
            binding.noticeText.show(updated)
        }
        collectFlow(viewModel.events) { event ->
            when (event) {
                is AuthEvent.NeedsCode -> findNavController().navigate(R.id.verifyCodeFragment)
                AuthEvent.NeedsNewPassword -> findNavController().navigate(R.id.newPasswordFragment)
                is AuthEvent.Error -> {
                    binding.noticeText.show(false)
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
        viewModel.consumePasswordUpdatedNotice()
        val email = binding.emailLayout.text.trim()
        val password = binding.passwordLayout.text
        var valid = true
        if (!Validators.isValidEmail(email)) {
            binding.emailLayout.setErrorIf(getString(R.string.auth_error_email)); valid = false
        }
        if (password.isEmpty()) {
            binding.passwordLayout.setErrorIf(getString(R.string.validation_required)); valid = false
        }
        if (valid) viewModel.signIn(email, password)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
