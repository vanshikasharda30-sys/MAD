package com.example.clinicai.ui.appointments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.clinicai.R
import com.example.clinicai.databinding.FragmentAppointmentDetailBinding
import com.example.clinicai.domain.ReceiptFormatter
import com.example.clinicai.data.model.DepositStatus
import com.example.clinicai.ui.booking.ReceiptActions
import com.example.clinicai.ui.booking.ReceiptRenderer
import com.example.clinicai.ui.common.StateViewController
import com.example.clinicai.ui.common.UiState
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.confirmDestructive
import com.example.clinicai.ui.common.setupBackToolbar
import com.example.clinicai.ui.common.show
import com.example.clinicai.ui.common.snack
import com.example.clinicai.ui.common.toMessage
import com.razorpay.Checkout
import org.json.JSONObject

/** One appointment: details, receipt, cancel, review and the optional deposit. */
class AppointmentDetailFragment : Fragment() {

    private var _binding: FragmentAppointmentDetailBinding? = null
    private val binding get() = _binding!!

    private val appointmentId by lazy { requireArguments().getString("appointmentId").orEmpty() }
    private val viewModel by appViewModels {
        AppointmentDetailViewModel(it.appointments, it.payments, it.receipts, it.paymentResults, appointmentId)
    }
    private val receiptActions = ReceiptActions(this)
    private lateinit var stateView: StateViewController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAppointmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackToolbar(binding.toolbar)
        stateView = StateViewController(binding.detailState, requireContext()) { viewModel.load() }

        collectFlow(viewModel.state) { state ->
            binding.content.show(state is UiState.Success)
            when (state) {
                UiState.Loading -> stateView.showLoading()
                is UiState.Error -> stateView.showError(state.error)
                is UiState.Empty -> stateView.showEmpty(getString(R.string.receipt_unavailable))
                is UiState.Success -> { stateView.hide(); bind(state.data) }
            }
        }
        collectFlow(viewModel.busy) { busy ->
            binding.progress.show(busy)
            listOf(binding.cancelButton, binding.reviewButton, binding.depositButton).forEach { it.isEnabled = !busy }
        }
        collectFlow(viewModel.events) { handle(it) }
    }

    private fun bind(detail: AppointmentDetail) {
        val receipt = detail.receipt
        val appointment = detail.appointment
        val document = ReceiptFormatter.format(receipt)
        binding.statusChip.text = document.statusLabel
        ReceiptRenderer.renderSections(binding.sectionsContainer, document.sections)

        binding.viewReceipt.setOnClickListener {
            findNavController().navigate(R.id.receiptFragment, bundleOf("appointmentId" to receipt.appointmentId))
        }
        binding.sharePdf.setOnClickListener { receiptActions.share(receipt) }
        binding.savePdf.setOnClickListener { receiptActions.save(receipt) }

        binding.cancelButton.show(appointment?.canCancel == true)
        binding.cancelButton.setOnClickListener {
            requireContext().confirmDestructive(
                title = getString(R.string.appts_cancel_confirm_title),
                message = getString(R.string.appts_cancel_confirm_body),
                confirmLabel = getString(R.string.appts_cancel),
                cancelLabel = getString(R.string.appts_cancel_keep),
            ) { viewModel.cancel() }
        }

        val canReview = appointment != null && appointment.status == "completed" && appointment.review == null
        binding.reviewButton.show(canReview)
        binding.reviewButton.setOnClickListener {
            ReviewDialog.show(requireContext()) { rating, comment -> viewModel.review(rating, comment) }
        }
        binding.ratedText.show(appointment?.review != null)
        appointment?.review?.let { binding.ratedText.text = getString(R.string.appts_rated, it.rating) }

        val canPay = appointment?.canPayDeposit == true
        binding.depositGroup.show(canPay)
        binding.depositButton.setOnClickListener { viewModel.startDeposit() }
        binding.depositPaid.show(appointment?.depositStatusEnum == DepositStatus.PAID)
    }

    private fun handle(event: DetailEvent) {
        when (event) {
            DetailEvent.Cancelled -> binding.root.snack(getString(R.string.appts_cancelled))
            DetailEvent.Reviewed -> binding.root.snack(getString(R.string.review_thanks))
            DetailEvent.DepositPaid -> binding.root.snack(getString(R.string.deposit_paid))
            DetailEvent.RatingMissing -> binding.root.snack(getString(R.string.review_pick_rating))
            is DetailEvent.Failed -> binding.root.snack(event.error.toMessage(requireContext()))
            is DetailEvent.PaymentFailed -> binding.root.snack(event.description ?: getString(R.string.deposit_failed))
            is DetailEvent.OpenCheckout -> openCheckout(event)
        }
    }

    /** Opens Razorpay's native checkout for the order the backend created. Results arrive via the host Activity. */
    private fun openCheckout(event: DetailEvent.OpenCheckout) {
        val order = event.order
        val keyId = order.keyId
        if (keyId.isNullOrBlank()) {
            binding.root.snack(getString(R.string.deposit_failed))
            return
        }
        try {
            val checkout = Checkout().apply { setKeyID(keyId) }
            val options = JSONObject().apply {
                put("name", getString(R.string.app_name))
                put("description", getString(R.string.deposit_title))
                put("order_id", order.orderId)
                put("currency", order.currency)
                put("amount", order.amount)
                put("theme", JSONObject().put("color", "#1F6F6B"))
            }
            checkout.open(requireActivity(), options)
        } catch (e: Exception) {
            binding.root.snack(getString(R.string.deposit_failed))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
