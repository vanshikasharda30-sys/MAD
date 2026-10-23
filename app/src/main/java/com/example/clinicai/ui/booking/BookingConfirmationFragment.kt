package com.example.clinicai.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.clinicai.R
import com.example.clinicai.databinding.FragmentBookingConfirmationBinding
import com.example.clinicai.domain.ReceiptFormatter
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.sharedAppViewModels

/** Shown only after the backend confirmed the appointment; displays the server-issued booking ID. */
class BookingConfirmationFragment : Fragment() {

    private var _binding: FragmentBookingConfirmationBinding? = null
    private val binding get() = _binding!!

    private val booking by sharedAppViewModels { BookingViewModel(it.appointments, it.receipts, it.prefs, it.auth) }
    private val receiptActions = ReceiptActions(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBookingConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })

        collectFlow(booking.state) { state ->
            val confirmed = state as? BookingState.Confirmed
            if (confirmed == null) {
                // Nothing confirmed in this session (e.g. process restart): never show a fake success.
                goHome()
                return@collectFlow
            }
            val receipt = confirmed.receipt
            val document = ReceiptFormatter.format(receipt)
            binding.bookingId.text = receipt.appointmentId
            ReceiptRenderer.renderRows(
                binding.summaryRows,
                document.sections.firstOrNull { it.title == "Appointment" }?.rows.orEmpty()
                    .filter { it.label in SUMMARY_LABELS },
            )
            binding.viewReceipt.setOnClickListener {
                findNavController().navigate(R.id.receiptFragment, bundleOf("appointmentId" to receipt.appointmentId))
            }
            binding.saveReceipt.setOnClickListener { receiptActions.save(receipt) }
            binding.shareReceipt.setOnClickListener { receiptActions.share(receipt) }
            binding.viewAppointments.setOnClickListener {
                booking.reset()
                val options = NavOptions.Builder().setPopUpTo(R.id.homeFragment, false).build()
                findNavController().navigate(R.id.myAppointmentsFragment, null, options)
            }
        }
    }

    private fun goHome() {
        booking.reset()
        findNavController().popBackStack(R.id.homeFragment, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        val SUMMARY_LABELS = setOf("Doctor", "Date & time", "Clinic")
    }
}
