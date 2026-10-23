package com.example.clinicai.ui.doctors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.example.clinicai.R
import com.example.clinicai.data.location.LocationStatus
import com.example.clinicai.data.model.Specialization
import com.example.clinicai.databinding.FragmentFindDoctorBinding
import com.example.clinicai.ui.common.LocationRequester
import com.example.clinicai.ui.common.StateViewController
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.container
import com.example.clinicai.ui.common.openDoctor
import com.example.clinicai.ui.common.show
import com.example.clinicai.domain.GeoDistance
import com.example.clinicai.ui.common.snack
import com.google.android.material.chip.Chip

class FindDoctorFragment : Fragment() {

    private var _binding: FragmentFindDoctorBinding? = null
    private val binding get() = _binding!!

    private val viewModel by appViewModels { FindDoctorViewModel(it.doctors, it.location, arguments?.getString("specializationId")) }
    private val locationRequester = LocationRequester(this) { container.location }
    private lateinit var stateView: StateViewController
    private lateinit var adapter: DoctorAdapter
    private var renderedSpecialties: List<Specialization>? = null
    private var lastLocation: LocationStatus? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFindDoctorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DoctorAdapter { openDoctor(it.id) }
        binding.doctorList.adapter = adapter
        stateView = StateViewController(binding.listState, requireContext()) { viewModel.load() }

        binding.searchInput.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
        binding.nearMeButton.setOnClickListener { locationRequester.request() }
        binding.clearLocationButton.setOnClickListener { viewModel.clearLocation() }

        collectFlow(viewModel.state) { render(it) }
    }

    private fun render(state: FindUiState) {
        val visible = state.visible
        adapter.location = state.location
        adapter.submitList(visible)

        when {
            state.loading -> stateView.showLoading()
            state.error != null -> stateView.showError(state.error)
            state.nothingNearby -> stateView.showEmpty(
                getString(R.string.find_nothing_nearby_title, GeoDistance.NEARBY_RADIUS_KM.toInt()),
                getString(R.string.find_nothing_nearby_body),
            )
            visible.isEmpty() -> stateView.showEmpty(getString(R.string.find_empty_title), getString(R.string.find_empty_body))
            else -> stateView.hide()
        }
        binding.doctorList.show(!state.loading && state.error == null && visible.isNotEmpty())

        renderSpecialtyChips(state)
        renderLocation(state.location)
    }

    private fun renderLocation(status: LocationStatus) {
        val locating = status == LocationStatus.Loading
        val active = status is LocationStatus.Available
        binding.nearMeButton.show(!active)
        binding.nearMeButton.isEnabled = !locating
        binding.nearMeButton.setText(if (locating) R.string.distance_loading else R.string.find_near_me)
        binding.nearMeStatus.show(active)
        binding.clearLocationButton.show(active)

        // Tell the user once per change why distances aren't showing.
        if (status != lastLocation) {
            when (status) {
                LocationStatus.PermissionDenied -> binding.root.snack(getString(R.string.find_location_denied), getString(R.string.location_open_settings)) { locationRequester.openAppSettings() }
                LocationStatus.Unavailable -> binding.root.snack(getString(R.string.find_location_unavailable), getString(R.string.action_retry)) { locationRequester.request() }
                else -> Unit
            }
            lastLocation = status
        }
    }

    private fun renderSpecialtyChips(state: FindUiState) {
        val specialties = state.specialties
        if (specialties != renderedSpecialties) {
            renderedSpecialties = specialties
            binding.specialtyChips.removeAllViews()
            binding.specialtyChips.addView(createChip(null, getString(R.string.find_all_specialties)))
            specialties.forEach { binding.specialtyChips.addView(createChip(it.id, it.name)) }
        }
        for (i in 0 until binding.specialtyChips.childCount) {
            val chip = binding.specialtyChips.getChildAt(i) as Chip
            chip.isChecked = (chip.tag as String?) == state.specializationId
        }
    }

    private fun createChip(id: String?, label: String): Chip =
        (layoutInflater.inflate(R.layout.view_filter_chip, binding.specialtyChips, false) as Chip).apply {
            text = label
            tag = id
            setOnClickListener { viewModel.selectSpecialty(id) }
        }

    override fun onResume() {
        super.onResume()
        viewModel.refreshLocationIfActive()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        lastLocation = null
    }
}
