package com.example.clinicai.ui.appointments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.clinicai.R
import com.example.clinicai.data.model.AppointmentStatus
import com.example.clinicai.databinding.ItemAppointmentBinding
import com.example.clinicai.domain.AppointmentItem
import com.example.clinicai.ui.common.show

class AppointmentAdapter(
    private val onOpen: (AppointmentItem) -> Unit,
    private val onCancel: (AppointmentItem) -> Unit,
    private val onReview: (AppointmentItem) -> Unit,
) : ListAdapter<AppointmentItem, AppointmentAdapter.Holder>(Diff) {

    inner class Holder(private val binding: ItemAppointmentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppointmentItem) {
            val context = binding.root.context
            binding.doctorName.text = item.doctorName
            binding.subtitle.text = item.subtitle
            binding.dateText.text = if (item.isWalkIn) "${item.dateText} · ${context.getString(R.string.appts_walk_in)}" else item.dateText
            binding.statusText.text = item.statusLabel
            binding.statusText.setTextColor(
                ContextCompat.getColor(
                    context,
                    when (item.status) {
                        AppointmentStatus.CONFIRMED, AppointmentStatus.IN_CONSULTATION -> R.color.teal
                        AppointmentStatus.CANCELLED -> R.color.red
                        AppointmentStatus.NO_SHOW -> R.color.amber
                        else -> R.color.ink_50
                    },
                ),
            )
            binding.cancelButton.show(item.canCancel)
            binding.cancelButton.setOnClickListener { onCancel(item) }

            binding.reviewButton.show(item.canReview)
            binding.reviewButton.setOnClickListener { onReview(item) }
            binding.ratedText.show(item.rating != null)
            item.rating?.let { binding.ratedText.text = context.getString(R.string.appts_rated, it) }

            binding.root.setOnClickListener { onOpen(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<AppointmentItem>() {
        override fun areItemsTheSame(a: AppointmentItem, b: AppointmentItem) = a.id == b.id
        override fun areContentsTheSame(a: AppointmentItem, b: AppointmentItem) = a == b
    }
}
