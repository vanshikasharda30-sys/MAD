package com.example.clinicai.ui.doctors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.clinicai.R
import com.example.clinicai.data.location.LocationStatus
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.domain.DoctorDistance
import com.example.clinicai.domain.GeoDistance
import com.example.clinicai.databinding.FragmentDoctorDetailBinding
import com.example.clinicai.ui.booking.BookingViewModel
import com.example.clinicai.ui.common.StateViewController
import com.example.clinicai.ui.common.UiState
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.LocationRequester
import com.example.clinicai.ui.common.composeEmail
import com.example.clinicai.ui.common.container
import com.example.clinicai.ui.common.dial
import com.example.clinicai.ui.common.openClinicInMaps
import com.example.clinicai.ui.common.openDirections
import com.example.clinicai.ui.common.setupBackToolbar
import com.example.clinicai.ui.common.sharedAppViewModels
import com.example.clinicai.ui.common.show
import com.example.clinicai.util.DateFormats

class DoctorDetailFragment : Fragment() {

    private var _binding: FragmentDoctorDetailBinding? = null
    private val binding get() = _binding!!

    private val doctorId by lazy { requireArguments().getString("doctorId").orEmpty() }
    private val viewModel by appViewModels { DoctorDetailViewModel(it.doctors, it.location, doctorId) }
    private val locationRequester = LocationRequester(this) { container.location }
    private val booking by sharedAppViewModels { BookingViewModel(it.appointments, it.receipts, it.prefs, it.auth) }

    private lateinit var doctorState: StateViewController
    private lateinit var slotState: StateViewController
    private lateinit var dateAdapter: DateStripAdapter
    private lateinit var slotAdapter: SlotAdapter
    private var loadedDoctor: Doctor? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDoctorDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackToolbar(binding.toolbar)

        doctorState = StateViewController(binding.doctorState, requireContext()) { viewModel.loadDoctor() }
        slotState = StateViewController(binding.slotState, requireContext()) { viewModel.loadSlots() }

        dateAdapter = DateStripAdapter { viewModel.selectDate(it.iso) }
        binding.dateStrip.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.dateStrip.adapter = dateAdapter
        dateAdapter.submitList(viewModel.days)

        slotAdapter = SlotAdapter { viewModel.selectSlot(it) }
        val grid = GridLayoutManager(requireContext(), SLOT_COLUMNS)
        grid.spanSizeLookup = slotAdapter.spanSizeLookup(SLOT_COLUMNS)
        binding.slotList.layoutManager = grid
        binding.slotList.adapter = slotAdapter

        binding.reviewButton.setOnClickListener { startReview() }

        collectFlow(viewModel.doctor) { renderDoctor(it) }
        collectFlow(viewModel.selectedDate) { dateAdapter.selectedIso = it }
        collectFlow(viewModel.slots) { renderSlots(it) }
        collectFlow(viewModel.distance) { renderDistance(it) }
        collectFlow(viewModel.locationStatus) { renderLocationAction(it) }
        collectFlow(viewModel.selectedSlot) { slot ->
            slotAdapter.selectedSlot = slot
            updateReviewButton(slot)
        }
    }

    private fun renderDoctor(state: UiState<Doctor>) {
        binding.content.show(state is UiState.Success)
        binding.bookingBar.show(state is UiState.Success)
        when (state) {
            UiState.Loading -> doctorState.showLoading()
            is UiState.Error -> doctorState.showError(state.error)
            is UiState.Empty -> doctorState.showEmpty(getString(R.string.doctor_not_found))
            is UiState.Success -> {
                doctorState.hide()
                loadedDoctor = state.data
                bind(state.data)
                updateReviewButton(viewModel.selectedSlot.value)
            }
        }
    }

    private fun bind(doctor: Doctor) {
        binding.toolbar.title = doctor.name
        binding.doctorInitials.text = doctor.initials
        binding.specialtyChip.text = doctor.specializationName
        binding.ratingText.show(doctor.hasReviews)
        binding.ratingText.text = "★ %.1f (%d)".format(doctor.avgRating, doctor.reviewCount)
        binding.doctorName.text = doctor.name
        binding.doctorQualification.text = "${doctor.qualification} · ${getString(R.string.doctor_experience, doctor.experienceYears)}"
        binding.languagesValue.text = doctor.languages.joinToString(", ")
        binding.slotLengthValue.text = getString(R.string.doctor_minutes, doctor.slotDurationMinutes)
        binding.clinicName.text = doctor.clinicName
        binding.clinicAddress.text = doctor.clinic?.let { "${it.address}, ${it.area}, ${it.city}" }.orEmpty()
        binding.aboutGroup.show(doctor.bio.isNotBlank())
        binding.aboutText.text = doctor.bio
        binding.feeText.text = getString(R.string.rupee_amount, DateFormats.rupees(doctor.consultationFee))
        bindContact(doctor)
    }

    /** Contact details are shown only when they pass validation; otherwise "Not available" and no action. */
    private fun bindContact(doctor: Doctor) {
        val clinic = doctor.clinic
        val phone = clinic?.contactPhone
        val email = clinic?.contactEmail
        binding.phoneValue.text = phone?.display ?: getString(R.string.contact_not_available)
        binding.emailValue.text = email ?: getString(R.string.contact_not_available)

        binding.directionsButton.show(clinic != null && (clinic.hasCoordinates || clinic.fullAddress.isNotBlank()))
        binding.directionsButton.setOnClickListener { clinic?.let { openDirections(it) } }
        binding.callButton.show(phone != null)
        binding.callButton.setOnClickListener { phone?.let { dial(it) } }
        binding.emailButton.show(email != null)
        binding.emailButton.setOnClickListener { email?.let { composeEmail(it, getString(R.string.contact_email_subject)) } }
        binding.clinicName.setOnClickListener { clinic?.let { openClinicInMaps(it) } }
    }

    private fun renderDistance(distance: DoctorDistance) {
        binding.distanceText.show(distance != DoctorDistance.Hidden)
        when (distance) {
            DoctorDistance.Hidden -> Unit
            is DoctorDistance.Known -> {
                binding.distanceText.text = getString(if (distance.approximate) R.string.distance_about else R.string.distance_from_you, GeoDistance.format(distance.km))
                binding.distanceText.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.teal))
            }
            DoctorDistance.Unavailable -> {
                binding.distanceText.text = getString(
                    if (viewModel.locationStatus.value == LocationStatus.PermissionDenied) R.string.distance_unavailable_denied else R.string.distance_unavailable,
                )
                binding.distanceText.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.ink_50))
            }
        }
    }

    /** The one button next to the distance: ask, retry, or go to Settings, depending on what went wrong. */
    private fun renderLocationAction(status: LocationStatus) {
        val action = binding.distanceAction
        when (status) {
            LocationStatus.NotRequested -> {
                action.show(true); action.isEnabled = true
                action.setText(R.string.distance_show)
                action.setOnClickListener { locationRequester.request() }
            }
            LocationStatus.Loading -> {
                action.show(true); action.isEnabled = false
                action.setText(R.string.distance_loading)
            }
            is LocationStatus.Available -> action.show(false)
            LocationStatus.PermissionDenied -> {
                action.show(true); action.isEnabled = true
                action.setText(R.string.location_settings)
                action.setOnClickListener {
                    // Ask again if the system still allows the prompt; the dialog explains why first.
                    locationRequester.request()
                }
            }
            LocationStatus.Unavailable -> {
                action.show(true); action.isEnabled = true
                action.setText(R.string.action_retry)
                action.setOnClickListener { locationRequester.request() }
            }
        }
    }

    private fun renderSlots(state: UiState<List<com.example.clinicai.domain.SlotGroup>>) {
        binding.slotList.show(state is UiState.Success)
        when (state) {
            UiState.Loading -> slotState.showLoading()
            is UiState.Error -> slotState.showError(state.error)
            is UiState.Empty -> slotState.showEmpty(getString(R.string.book_no_slots_title), getString(R.string.book_no_slots_body))
            is UiState.Success -> {
                slotState.hide()
                slotAdapter.submit(state.data)
            }
        }
    }

    private fun updateReviewButton(slot: String?) {
        val fee = loadedDoctor?.consultationFee
        binding.reviewButton.isEnabled = slot != null && loadedDoctor != null
        binding.reviewButton.text = if (slot == null) {
            getString(R.string.book_select_slot_first)
        } else {
            getString(R.string.book_review_and_confirm) + (fee?.let { " · ₹${DateFormats.rupees(it)}" } ?: "")
        }
    }

    private fun startReview() {
        val doctor = loadedDoctor ?: return
        val slot = viewModel.selectedSlot.value ?: return
        booking.startDraft(doctor, viewModel.selectedDate.value, slot)
        findNavController().navigate(R.id.bookingReviewFragment)
    }

    override fun onResume() {
        super.onResume()
        // Returning from the booking flow: the slot just taken (by us or someone else) must disappear.
        if (booking.consumeSlotsStale()) viewModel.loadSlots()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val SLOT_COLUMNS = 3
    }
}
