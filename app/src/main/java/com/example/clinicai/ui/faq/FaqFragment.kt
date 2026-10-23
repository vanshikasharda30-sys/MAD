package com.example.clinicai.ui.faq

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.example.clinicai.R
import com.example.clinicai.databinding.FragmentFaqBinding
import com.example.clinicai.ui.common.StateViewController
import com.example.clinicai.ui.common.UiState
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.setupBackToolbar
import com.example.clinicai.ui.common.show

/** A clinic's FAQs (`GET /api/clinics/{id}/faqs`) — the same answers the chat receptionist uses. */
class FaqFragment : Fragment() {

    private var _binding: FragmentFaqBinding? = null
    private val binding get() = _binding!!

    private val viewModel by appViewModels { FaqViewModel(it.clinics, it.doctors) }
    private val adapter = FaqAdapter()
    private lateinit var stateView: StateViewController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFaqBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackToolbar(binding.toolbar)
        binding.faqList.adapter = adapter
        stateView = StateViewController(binding.faqState, requireContext()) { viewModel.load() }

        collectFlow(viewModel.state) { state ->
            binding.clinicPicker.show(state is UiState.Success)
            binding.faqList.show(state is UiState.Success)
            when (state) {
                UiState.Loading -> stateView.showLoading()
                is UiState.Error -> stateView.showError(state.error)
                is UiState.Empty -> stateView.showEmpty(getString(R.string.faq_empty))
                is UiState.Success -> {
                    val screen = state.data
                    bindPicker(screen)
                    adapter.submitList(screen.faqs)
                    if (screen.faqs.isEmpty()) stateView.showEmpty(getString(R.string.faq_empty)) else stateView.hide()
                }
            }
        }
    }

    private fun bindPicker(screen: FaqScreen) {
        val names = screen.clinics.map { it.name }
        binding.clinicDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names))
        binding.clinicDropdown.setText(screen.selected?.name.orEmpty(), false)
        binding.clinicDropdown.setOnItemClickListener { _, _, position, _ ->
            screen.clinics.getOrNull(position)?.let(viewModel::select)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
