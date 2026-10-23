package com.example.clinicai.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.clinicai.R
import com.example.clinicai.databinding.FragmentReceiptBinding
import com.example.clinicai.domain.ReceiptData
import com.example.clinicai.domain.ReceiptFormatter
import com.example.clinicai.ui.common.StateViewController
import com.example.clinicai.ui.common.UiState
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.setupBackToolbar
import com.example.clinicai.ui.common.show

/** On-screen booking confirmation bill, with Save PDF / Share / Open PDF. */
class ReceiptFragment : Fragment() {

    private var _binding: FragmentReceiptBinding? = null
    private val binding get() = _binding!!

    private val appointmentId by lazy { requireArguments().getString("appointmentId").orEmpty() }
    private val viewModel by appViewModels { ReceiptViewModel(it.appointments, it.receipts, appointmentId) }
    private val actions = ReceiptActions(this)
    private lateinit var stateView: StateViewController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReceiptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackToolbar(binding.toolbar)
        stateView = StateViewController(binding.receiptState, requireContext()) { viewModel.load() }

        collectFlow(viewModel.state) { state ->
            binding.receiptContent.show(state is UiState.Success)
            binding.actionBar.show(state is UiState.Success)
            when (state) {
                UiState.Loading -> stateView.showLoading()
                is UiState.Error -> stateView.showError(state.error)
                is UiState.Empty -> stateView.showEmpty(getString(R.string.receipt_unavailable))
                is UiState.Success -> {
                    stateView.hide()
                    bind(state.data)
                }
            }
        }
    }

    private fun bind(receipt: ReceiptData) {
        val document = ReceiptFormatter.format(receipt)
        binding.receiptTitle.text = document.title
        binding.receiptSubtitle.text = document.subtitle
        binding.receiptStatus.text = document.statusLabel
        ReceiptRenderer.renderSections(binding.sectionsContainer, document.sections)

        binding.noticesContainer.removeAllViews()
        document.notices.forEach { notice ->
            binding.noticesContainer.addView(
                TextView(requireContext()).apply {
                    setTextAppearance(R.style.TextAppearance_ClinicAI_Body)
                    text = notice
                    setBackgroundResource(R.drawable.bg_pill_teal)
                    val pad = (12 * resources.displayMetrics.density).toInt()
                    setPadding(pad, pad, pad, pad)
                    layoutParams = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply { topMargin = pad }
                },
            )
        }

        binding.savePdf.setOnClickListener { actions.save(receipt) }
        binding.sharePdf.setOnClickListener { actions.share(receipt) }
        binding.openPdf.setOnClickListener { actions.open(receipt) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
