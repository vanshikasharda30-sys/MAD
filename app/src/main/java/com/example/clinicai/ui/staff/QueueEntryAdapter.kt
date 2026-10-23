package com.example.clinicai.ui.staff

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.clinicai.R
import com.example.clinicai.data.model.WaitingEntry
import com.example.clinicai.data.repository.QueueRepository.QueueAction
import com.example.clinicai.databinding.ItemQueueEntryBinding

class QueueEntryAdapter(
    private val onAction: (WaitingEntry, QueueAction) -> Unit,
) : ListAdapter<WaitingEntry, QueueEntryAdapter.Holder>(Diff) {

    /** Disable the action buttons while any request is in flight. */
    var busy: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class Holder(private val binding: ItemQueueEntryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: WaitingEntry) {
            val context = binding.root.context
            binding.ticket.text = context.getString(R.string.queue_ticket, entry.queueNumber)
            binding.wait.text = context.getString(R.string.queue_wait_est, entry.estimatedWaitMinutes)
            listOf(binding.callButton, binding.skipButton, binding.completeButton).forEach { it.isEnabled = !busy }
            binding.callButton.setOnClickListener { onAction(entry, QueueAction.CALL) }
            binding.skipButton.setOnClickListener { onAction(entry, QueueAction.SKIP) }
            binding.completeButton.setOnClickListener { onAction(entry, QueueAction.COMPLETE) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemQueueEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<WaitingEntry>() {
        override fun areItemsTheSame(a: WaitingEntry, b: WaitingEntry) = a.queueEntryId == b.queueEntryId
        override fun areContentsTheSame(a: WaitingEntry, b: WaitingEntry) = a == b
    }
}
