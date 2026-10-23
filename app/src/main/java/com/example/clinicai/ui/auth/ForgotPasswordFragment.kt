package com.example.clinicai.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.clinicai.R
import com.example.clinicai.databinding.FragmentForgotPasswordBinding
import com.example.clinicai.ui.common.clearErrorOnChange
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.hideKeyboard
import com.example.clinicai.ui.common.setErrorIf
import com.example.clinicai.ui.common.show
import com.example.clinicai.ui.common.text
import com.example.clinicai.util.Validators

/** Step 1 of password recovery: ask for the e-mail address and have Clerk send a one-time code. */
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel get() = (requireActivity() as AuthActivity).viewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.emailInput.clearErrorOnChange(binding.emailLayout)
        binding.sendButton.setOnClickListener { submit() }
        binding.backToSignIn.setOnClickListener { findNavController().navigateUp() }

        collectFlow(viewModel.busy) { busy ->
            binding.sendButton.isEnabled = !busy
            binding.progress.show(busy)
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
        val email = binding.emailLayout.text.trim()
        if (!Validators.isValidEmail(email)) {
            binding.emailLayout.setErrorIf(getString(R.string.auth_error_email))
            return
        }
        viewModel.startPasswordReset(email)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
