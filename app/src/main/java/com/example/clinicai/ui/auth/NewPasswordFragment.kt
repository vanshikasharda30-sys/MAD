package com.example.clinicai.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.clinicai.R
import com.example.clinicai.databinding.FragmentNewPasswordBinding
import com.example.clinicai.ui.common.clearErrorOnChange
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.hideKeyboard
import com.example.clinicai.ui.common.setErrorIf
import com.example.clinicai.ui.common.show
import com.example.clinicai.ui.common.text

/**
 * Choose a new password, after the reset code was verified (forgot password) or when Clerk requires
 * a change during sign-in. Shows a success message only after Clerk has confirmed the change.
 */
class NewPasswordFragment : Fragment() {

    private var _binding: FragmentNewPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel get() = (requireActivity() as AuthActivity).viewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNewPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.passwordInput.clearErrorOnChange(binding.passwordLayout)
        binding.confirmInput.clearErrorOnChange(binding.confirmLayout)
        binding.newPasswordBody.setText(
            if (viewModel.passwordChangeIsForced) R.string.new_password_body_forced else R.string.new_password_body_reset,
        )
        binding.updateButton.setOnClickListener { submit() }

        collectFlow(viewModel.policy) { binding.passwordLayout.helperText = it.hintText(requireContext()) }
        collectFlow(viewModel.busy) { busy ->
            binding.updateButton.isEnabled = !busy
            binding.progress.show(busy)
        }
        collectFlow(viewModel.events) { event ->
            when (event) {
                // Forgot-password finished: back to sign-in, where the "password updated" notice is shown.
                AuthEvent.PasswordResetDone -> findNavController().popBackStack(R.id.signInFragment, false)
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
        val password = binding.passwordLayout.text
        val confirm = binding.confirmLayout.text
        var valid = true
        viewModel.policy.value.errorFor(requireContext(), password)?.let { binding.passwordLayout.setErrorIf(it); valid = false }
        if (password != confirm) { binding.confirmLayout.setErrorIf(getString(R.string.new_password_mismatch)); valid = false }
        if (valid) viewModel.submitNewPassword(password)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
