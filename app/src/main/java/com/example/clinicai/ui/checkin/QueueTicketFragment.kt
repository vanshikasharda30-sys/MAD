package com.example.clinicai.ui.checkin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.clinicai.R
import com.example.clinicai.config.AppConstants
import com.example.clinicai.databinding.FragmentQueueTicketBinding
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.pollWhileStarted
import com.example.clinicai.ui.common.show

/** The walk-in ticket, refreshed from `GET /api/queue/{doctorId}/status` while on screen. */
class QueueTicketFragment : Fragment() {

    private var _binding: FragmentQueueTicketBinding? = null
    private val binding get() = _binding!!

    private val doctorId by lazy { requireArguments().getString("doctorId").orEmpty() }
    private val ticket by lazy { requireArguments().getInt("queueNumber") }
    private val doctorName by lazy { requireArguments().getString("doctorName").orEmpty() }
    private val viewModel by appViewModels { QueueTicketViewModel(it.queue, doctorId, ticket) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQueueTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ticketNumber.text = "#$ticket"
        binding.doctorName.text = doctorName
        binding.liveNote.text = getString(R.string.checkin_live)

        // The ticket is not tied to a previous screen; back goes home rather than back into the join form.
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack(R.id.homeFragment, false)
            }
        })
        binding.doneButton.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }

        pollWhileStarted(AppConstants.QUEUE_POLL_INTERVAL_MS) { viewModel.refresh() }

        collectFlow(viewModel.progress) { progress ->
            progress ?: return@collectFlow
            binding.nowServing.show(progress.nowServing != null)
            progress.nowServing?.let { binding.nowServing.text = getString(R.string.checkin_now_serving, it) }

            binding.yourTurn.show(progress.isYourTurn)
            val ahead = progress.peopleAhead
            binding.peopleAhead.show(ahead != null && !progress.isYourTurn)
            if (ahead != null) {
                binding.peopleAhead.text =
                    if (ahead == 0) getString(R.string.checkin_next) else getString(R.string.checkin_people_ahead, ahead)
            }
            binding.waitEstimate.show(progress.estimatedWaitMinutes != null && !progress.isYourTurn)
            progress.estimatedWaitMinutes?.let { binding.waitEstimate.text = getString(R.string.checkin_wait, it) }
        }
        collectFlow(viewModel.offline) { binding.offlineNote.show(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
