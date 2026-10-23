package com.example.clinicai.ui.booking

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.clinicai.R
import com.example.clinicai.data.auth.AuthState
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.databinding.FragmentBookingReviewBinding
import com.example.clinicai.domain.ReceiptRow
import com.example.clinicai.ui.common.authIntent
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.hideKeyboard
import com.example.clinicai.ui.common.setupBackToolbar
import com.example.clinicai.ui.common.sharedAppViewModels
import com.example.clinicai.ui.common.show
import com.example.clinicai.ui.common.snack
import com.example.clinicai.ui.common.toMessage
import com.example.clinicai.util.DateFormats
import com.example.clinicai.util.Validators
import kotlinx.coroutines.flow.combine

/**
 * Step 4 of the flow: review the details, then submit to the backend. The confirmation screen is
 * opened only when [BookingState.Confirmed] arrives, which the view model emits solely after the
 * server created the appointment.
 */
class BookingReviewFragment : Fragment() {

    private var _binding: FragmentBookingReviewBinding? = null
    private val binding get() = _binding!!

    private val booking by sharedAppViewModels { BookingViewModel(it.appointments, it.receipts, it.prefs, it.auth) }

    // After signing in the user lands back here with the draft intact (it lives in the shared view model)
    // and confirms explicitly; nothing is submitted on their behalf.
    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && _binding != null) {
            binding.root.snack(getString(R.string.book_signed_in_continue))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBookingReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackToolbar(binding.toolbar)

        binding.notesInput.doAfterTextChanged { booking.setNotes(it?.toString().orEmpty()) }
        binding.confirmButton.setOnClickListener { onConfirmClicked() }
        binding.chooseAnotherButton.setOnClickListener {
            booking.dismissFailure()
            findNavController().navigateUp()
        }

        collectFlow(combine(booking.draft, booking.authState) { draft, auth -> draft to auth }) { (draft, auth) ->
            if (draft == null) {
                findNavController().navigateUp()
                return@collectFlow
            }
            val rows = buildList {
                add(ReceiptRow(getString(R.string.book_review_doctor), draft.doctor.name, emphasized = true))
                draft.doctor.specialization?.name?.let { add(ReceiptRow("Specialty", it)) }
                draft.doctor.clinic?.let { add(ReceiptRow(getString(R.string.book_review_clinic), it.name)) }
                add(
                    ReceiptRow(
                        getString(R.string.book_review_when),
                        "${DateFormats.longDate(draft.date)} · ${DateFormats.time12h(draft.startTime)}",
                    ),
                )
                val patient = if (auth is AuthState.SignedIn) {
                    listOfNotNull(booking.accountName(), booking.accountEmail()).joinToString("\n")
                } else {
                    ""
                }
                if (patient.isNotBlank()) add(ReceiptRow(getString(R.string.book_review_patient), patient))
                add(ReceiptRow(getString(R.string.book_review_fee), getString(R.string.rupee_amount, DateFormats.rupees(draft.doctor.consultationFee))))
            }
            ReceiptRenderer.renderRows(binding.summaryRows, rows)
        }

        collectFlow(booking.notes) { notes ->
            if (binding.notesInput.text?.toString() != notes) binding.notesInput.setText(notes)
        }

        collectFlow(combine(booking.state, booking.authState) { state, auth -> state to auth }) { (state, auth) ->
            render(state, auth)
        }
    }

    private fun onConfirmClicked() {
        binding.root.hideKeyboard()
        if (booking.authState.value is AuthState.SignedOut) {
            signInLauncher.launch(authIntent(requireContext()))
        } else {
            booking.dismissFailure()
            booking.submit()
        }
    }

    private fun render(state: BookingState, auth: AuthState) {
        val submitting = state is BookingState.Submitting
        val checking = auth is AuthState.Initializing
        binding.progress.show(submitting || checking)
        // While the saved session is still being restored we neither claim "signed out" nor allow a submit.
        binding.confirmButton.isEnabled = !submitting && !checking
        binding.notesLayout.isEnabled = !submitting
        binding.confirmButton.text = when {
            submitting -> getString(R.string.book_confirming)
            checking -> getString(R.string.book_checking_account)
            auth is AuthState.SignedOut -> getString(R.string.book_sign_in_to_confirm)
            else -> getString(R.string.book_confirm)
        }
        binding.signInHint.show(auth is AuthState.SignedOut)

        when (state) {
            is BookingState.Confirmed -> {
                val options = NavOptions.Builder().setPopUpTo(R.id.bookingReviewFragment, true).build()
                findNavController().navigate(R.id.bookingConfirmationFragment, null, options)
            }
            is BookingState.Failed -> showFailure(state)
            else -> {
                binding.errorText.show(false)
                binding.chooseAnotherButton.show(false)
                binding.notesLayout.error = null
            }
        }
    }

    private fun showFailure(state: BookingState.Failed) {
        val message = when {
            state.formProblem == Validators.BookingCheck.NotesTooLong -> getString(R.string.book_error_notes_too_long)
            state.formProblem != null -> getString(R.string.book_error_pick_slot)
            state.outcomeUnknown -> getString(R.string.book_uncertain)
            state.error is AppError.Conflict -> {
                val server = state.error.serverMessage ?: getString(R.string.book_error_slot_taken)
                "$server ${getString(R.string.book_slot_taken_hint)}"
            }
            state.error is AppError.Validation -> {
                val server = state.error.serverMessage ?: getString(R.string.book_error_slot_taken)
                "$server ${getString(R.string.book_slot_taken_hint)}"
            }
            state.error != null -> state.error.toMessage(requireContext())
            else -> getString(R.string.error_generic)
        }
        binding.errorText.text = message
        binding.errorText.show(true)
        // A taken slot can't be fixed by retrying: send the user back to pick another one.
        binding.chooseAnotherButton.show(state.error is AppError.Conflict)
        // The server rejected the session itself: ask the user to sign in again instead of retrying blindly.
        if (state.error?.isAuthProblem == true) binding.signInHint.show(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
