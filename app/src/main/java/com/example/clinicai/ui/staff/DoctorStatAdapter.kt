package com.example.clinicai.ui.staff

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.clinicai.R
import com.example.clinicai.data.model.DoctorStat
import com.example.clinicai.databinding.ItemDoctorStatBinding

class DoctorStatAdapter : ListAdapter<DoctorStat, DoctorStatAdapter.Holder>(Diff) {

    var maxCount: Int = 1

    inner class Holder(private val binding: ItemDoctorStatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stat: DoctorStat) {
            val context = binding.root.context
            binding.statName.text = stat.name
            binding.statDetail.text = context.getString(R.string.analytics_doctor_row, stat.appointmentCount, stat.noShowCount) +
                if (stat.reviewCount > 0) "  ·  ★ %.1f".format(stat.avgRating) else ""
            binding.statBar.max = maxCount.coerceAtLeast(1)
            binding.statBar.progress = stat.appointmentCount
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemDoctorStatBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<DoctorStat>() {
        override fun areItemsTheSame(a: DoctorStat, b: DoctorStat) = a.id == b.id
        override fun areContentsTheSame(a: DoctorStat, b: DoctorStat) = a == b
    }
}
