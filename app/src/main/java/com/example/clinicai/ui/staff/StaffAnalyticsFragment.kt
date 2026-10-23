package com.example.clinicai.ui.staff

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.clinicai.R
import com.example.clinicai.data.model.AnalyticsData
import com.example.clinicai.databinding.FragmentStaffAnalyticsBinding
import com.example.clinicai.domain.AnalyticsCsv
import com.example.clinicai.ui.common.StateViewController
import com.example.clinicai.ui.common.UiState
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.show
import com.example.clinicai.ui.common.snack
import com.example.clinicai.util.DateFormats
import java.io.File

/** Last-30-days KPIs, 14-day trend and the doctor ledger, with CSV export. */
class StaffAnalyticsFragment : Fragment() {

    private var _binding: FragmentStaffAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel by appViewModels { StaffAnalyticsViewModel(it.staff) }
    private val adapter = DoctorStatAdapter()
    private lateinit var stateView: StateViewController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStaffAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.statList.adapter = adapter
        stateView = StateViewController(binding.analyticsState, requireContext()) { viewModel.load() }
        binding.refresh.setOnRefreshListener { viewModel.load() }

        collectFlow(viewModel.state) { state ->
            binding.refresh.isRefreshing = false
            binding.content.show(state is UiState.Success)
            when (state) {
                UiState.Loading -> stateView.showLoading()
                is UiState.Error -> stateView.showError(state.error)
                is UiState.Empty -> stateView.hide()
                is UiState.Success -> { stateView.hide(); bind(state.data) }
            }
        }
    }

    private fun bind(data: AnalyticsData) {
        binding.rangeText.text = getString(
            R.string.analytics_range,
            DateFormats.shortDate(data.rangeFrom),
            DateFormats.shortDate(data.rangeTo),
        )
        binding.totalValue.text = data.total.toString()
        binding.completionValue.text = "%.0f%%".format(data.completionRate)
        binding.noShowValue.text = "%.0f%%".format(data.noShowRate)
        binding.revenueValue.text = getString(R.string.rupee_amount, DateFormats.rupees(data.estimatedRevenue))
        binding.ratingValue.text = if (data.reviewCount > 0) "%.1f".format(data.avgRating) else "—"
        binding.breakdown.text = getString(R.string.analytics_breakdown, data.completed, data.noShow, data.cancelled) +
            "\n" + getString(R.string.analytics_mix, data.scheduled, data.walkIn)

        binding.trendChart.setData(data.dailyTrend)
        adapter.maxCount = data.doctorStats.maxOfOrNull { it.appointmentCount } ?: 1
        adapter.submitList(data.doctorStats)

        binding.exportButton.setOnClickListener { export(data) }
    }

    private fun export(data: AnalyticsData) {
        try {
            val dir = File(requireContext().cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, "clinicai-doctor-ledger.csv")
            file.writeText(AnalyticsCsv.build(data.doctorStats))
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, getString(R.string.analytics_export)))
        } catch (e: Exception) {
            binding.root.snack(getString(R.string.error_generic))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
