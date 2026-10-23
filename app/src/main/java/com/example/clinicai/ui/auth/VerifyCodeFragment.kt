package com.example.clinicai.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.clinicai.R
import com.example.clinicai.databinding.FragmentVerifyCodeBinding
import com.example.clinicai.ui.common.clearErrorOnChange
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.hideKeyboard
import com.example.clinicai.ui.common.setErrorIf
import com.example.clinicai.ui.common.show
import com.example.clinicai.ui.common.snack
import com.example.clinicai.ui.common.text
import com.example.clinicai.util.Validators

/**
 * Asks for the one-time code for whatever Clerk required: e-mail verification at sign-up, the
 * new-device ("client trust") or MFA check at sign-in, or the password-reset code. Wording comes
 * from the pending [com.example.clinicai.data.auth.CodeChallenge].
 */
class VerifyCodeFragment : Fragment() {

    private var _binding: FragmentVerifyCodeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel get() = (requireActivity() as AuthActivity).viewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVerifyCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.codeInput.clearErrorOnChange(binding.codeLayout)
        binding.verifyButton.setOnClickListener { submit() }
        binding.resendButton.setOnClickListener { viewModel.resendCode() }
        binding.cancelButton.setOnClickListener {
            viewModel.cancelFlow()
            findNavController().navigateUp()
        }

        collectFlow(viewModel.challenge) { challenge ->
            if (challenge == null) return@collectFlow
            binding.verifyTitle.text = challenge.title(requireContext())
            binding.verifyBody.text = challenge.body(requireContext())
            binding.resendButton.show(challenge.canResend)
        }
        collectFlow(viewModel.busy) { busy ->
            binding.verifyButton.isEnabled = !busy
            binding.progress.show(busy)
        }
        collectFlow(viewModel.resendIn) { seconds ->
            binding.resendButton.isEnabled = seconds == 0 && !viewModel.busy.value
            binding.resendButton.text =
                if (seconds > 0) getString(R.string.auth_resend_in, seconds) else getString(R.string.auth_resend)
        }
        collectFlow(viewModel.events) { event ->
            when (event) {
                AuthEvent.CodeResent -> binding.root.snack(getString(R.string.auth_code_resent))
                AuthEvent.NeedsNewPassword -> findNavController().navigate(R.id.newPasswordFragment)
                AuthEvent.PasswordResetDone -> Unit
                is AuthEvent.NeedsCode -> {
                    // A further challenge (e.g. second factor after e-mail code): same screen, new wording.
                    binding.codeInput.setText("")
                    binding.errorText.show(false)
                }
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
        val code = binding.codeLayout.text.trim()
        if (!Validators.isValidVerificationCode(code)) {
            binding.codeLayout.setErrorIf(getString(R.string.auth_error_code))
        } else {
            viewModel.submitCode(code)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
