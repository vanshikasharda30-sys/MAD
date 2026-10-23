package com.example.clinicai.ui.checkin

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import com.example.clinicai.R
import com.example.clinicai.data.auth.AuthState
import com.example.clinicai.ui.common.container
import kotlinx.coroutines.launch
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.databinding.FragmentCheckinBinding
import com.example.clinicai.ui.common.StateViewController
import com.example.clinicai.ui.common.UiState
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.authIntent
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.hideKeyboard
import com.example.clinicai.ui.common.setErrorIf
import com.example.clinicai.ui.common.setupBackToolbar
import com.example.clinicai.ui.common.show
import com.example.clinicai.ui.common.snack
import com.example.clinicai.ui.common.text
import com.example.clinicai.ui.common.toMessage
import com.example.clinicai.util.CheckinTokenParser
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/** Walk-in check-in: scan or enter the clinic's QR code, pick a doctor, join the live queue. */
class CheckinFragment : Fragment() {

    private var _binding: FragmentCheckinBinding? = null
    private val binding get() = _binding!!

    private val viewModel by appViewModels { CheckinViewModel(it.clinics, it.queue, it.auth) }
    private lateinit var adapter: CheckinDoctorAdapter
    private lateinit var stateView: StateViewController
    private var pendingDoctor: Doctor? = null

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val doctor = pendingDoctor
        pendingDoctor = null
        if (result.resultCode == Activity.RESULT_OK && doctor != null) viewModel.join(doctor)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCheckinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackToolbar(binding.toolbar)

        adapter = CheckinDoctorAdapter { doctor -> onJoin(doctor) }
        binding.doctorList.adapter = adapter
        stateView = StateViewController(binding.clinicState, requireContext()) { viewModel.retry() }

        binding.scanButton.setOnClickListener { scan() }
        binding.lookupButton.setOnClickListener { lookupFromInput() }
        binding.codeInput.setOnEditorActionListener { _, _, _ -> lookupFromInput(); true }

        // Deep link / clinic list: /checkin/{token}
        arguments?.getString("token")?.takeIf { it.isNotBlank() && savedInstanceState == null }?.let { viewModel.lookup(it) }

        collectFlow(viewModel.clinic) { render(it) }
        collectFlow(viewModel.joiningDoctorId) { adapter.joiningDoctorId = it }
        collectFlow(viewModel.events) { event ->
            when (event) {
                is CheckinEvent.Joined -> findNavController().navigate(
                    R.id.queueTicketFragment,
                    bundleOf(
                        "doctorId" to event.doctor.id,
                        "queueNumber" to event.queueNumber,
                        "doctorName" to event.doctor.name,
                    ),
                )
                is CheckinEvent.JoinFailed -> binding.root.snack(event.error.toMessage(requireContext()))
            }
        }
    }

    private fun render(state: UiState<com.example.clinicai.data.model.Clinic>) {
        binding.clinicGroup.show(state is UiState.Success)
        when (state) {
            UiState.Loading -> stateView.showLoading()
            is UiState.Empty -> stateView.hide()
            is UiState.Error ->
                if (state.error is AppError.NotFound) stateView.showEmpty(getString(R.string.checkin_invalid_code)) else stateView.showError(state.error)
            is UiState.Success -> {
                stateView.hide()
                val clinic = state.data
                binding.clinicName.text = clinic.name
                binding.clinicLocality.text = clinic.locality
                val doctors = clinic.doctors.orEmpty()
                adapter.submitList(doctors)
                binding.noDoctors.show(doctors.isEmpty())
            }
        }
    }

    private fun onJoin(doctor: Doctor) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Wait for any saved session to be restored before deciding the user is signed out.
            if (container.auth.awaitReady() is AuthState.SignedIn) {
                viewModel.join(doctor)
            } else {
                pendingDoctor = doctor
                signInLauncher.launch(authIntent(requireContext()))
            }
        }
    }

    private fun lookupFromInput() {
        binding.root.hideKeyboard()
        val token = CheckinTokenParser.parse(binding.codeLayout.text)
        if (token == null) {
            binding.codeLayout.setErrorIf(getString(R.string.validation_required))
        } else {
            binding.codeLayout.setErrorIf(null)
            viewModel.lookup(token)
        }
    }

    private fun scan() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        GmsBarcodeScanning.getClient(requireContext(), options)
            .startScan()
            .addOnSuccessListener { barcode ->
                if (_binding == null) return@addOnSuccessListener
                val token = CheckinTokenParser.parse(barcode.rawValue)
                if (token != null) viewModel.lookup(token) else binding.root.snack(getString(R.string.checkin_invalid_code))
            }
            .addOnFailureListener {
                if (_binding != null) binding.root.snack(getString(R.string.checkin_scan_unavailable))
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
