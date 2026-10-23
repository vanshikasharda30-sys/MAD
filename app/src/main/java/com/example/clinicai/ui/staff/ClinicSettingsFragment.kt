package com.example.clinicai.ui.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.clinicai.BuildConfig
import com.example.clinicai.R
import com.example.clinicai.config.AppConstants
import com.example.clinicai.data.model.Clinic
import com.example.clinicai.databinding.FragmentClinicSettingsBinding
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
import com.example.clinicai.util.QrCodeGenerator
import com.example.clinicai.domain.ContactValidator

class ClinicSettingsFragment : Fragment() {

    private var _binding: FragmentClinicSettingsBinding? = null
    private val binding get() = _binding!!

    private val session by sharedAppViewModels { StaffSessionViewModel(it.users, it.auth) }
    private val viewModel by appViewModels { ClinicSettingsViewModel(it.clinics) }
    private lateinit var stateView: StateViewController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentClinicSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stateView = StateViewController(binding.settingsState, requireContext()) { viewModel.load() }
        binding.phoneInput.clearErrorOnChange(binding.phoneLayout)
        binding.emailInput.clearErrorOnChange(binding.emailLayout)
        binding.saveButton.show(session.isAdmin)
        binding.readOnlyNote.show(!session.isAdmin)
        binding.phoneLayout.isEnabled = session.isAdmin
        binding.emailLayout.isEnabled = session.isAdmin
        binding.saveButton.setOnClickListener { save() }

        collectFlow(viewModel.state) { state ->
            binding.content.show(state is UiState.Success)
            when (state) {
                UiState.Loading -> stateView.showLoading()
                is UiState.Error -> stateView.showError(state.error)
                is UiState.Empty -> stateView.hide()
                is UiState.Success -> { stateView.hide(); bind(state.data) }
            }
        }
        collectFlow(viewModel.saving) { binding.saveButton.isEnabled = !it }
        collectFlow(viewModel.events) { event ->
            when (event) {
                SettingsEvent.Saved -> binding.root.snack(getString(R.string.settings_saved))
                is SettingsEvent.Failed -> binding.root.snack(event.error.toMessage(requireContext()))
            }
        }
    }

    private fun bind(clinic: Clinic) {
        binding.nameInput.setText(clinic.name)
        binding.addressInput.setText(clinic.fullAddress)
        if (binding.phoneInput.text?.toString() != clinic.phone) binding.phoneInput.setText(clinic.phone)
        if (binding.emailInput.text?.toString() != clinic.email) binding.emailInput.setText(clinic.email)
        // Tell the admin when the stored contact details are placeholders that patients never see.
        binding.phoneLayout.helperText = if (clinic.contactPhone == null) getString(R.string.settings_placeholder_phone) else null
        binding.emailLayout.helperText = if (clinic.contactEmail == null) getString(R.string.settings_placeholder_email) else null

        val token = clinic.qrToken
        binding.qrGroup.show(!token.isNullOrBlank())
        if (!token.isNullOrBlank()) {
            // The QR encodes the website's public check-in link, so any phone camera can use it too.
            val link = BuildConfig.PROD_API_BASE_URL + AppConstants.CHECKIN_PATH + "/" + token
            val size = (220 * resources.displayMetrics.density).toInt()
            binding.qrImage.setImageBitmap(QrCodeGenerator.create(link, size))
            binding.qrLink.text = link
        }
    }

    private fun save() {
        binding.root.hideKeyboard()
        val phone = binding.phoneLayout.text.trim()
        val email = binding.emailLayout.text.trim()
        var valid = true
        if (ContactValidator.phone(phone) == null) { binding.phoneLayout.setErrorIf(getString(R.string.settings_placeholder_phone)); valid = false }
        if (ContactValidator.email(email) == null) { binding.emailLayout.setErrorIf(getString(R.string.settings_placeholder_email)); valid = false }
        if (valid) viewModel.save(phone, email)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
