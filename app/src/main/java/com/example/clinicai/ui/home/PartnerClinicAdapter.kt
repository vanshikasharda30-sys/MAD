package com.example.clinicai.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.clinicai.data.model.Clinic
import com.example.clinicai.databinding.ItemPartnerClinicBinding

class PartnerClinicAdapter(
    private val onClick: (Clinic) -> Unit,
) : ListAdapter<Clinic, PartnerClinicAdapter.Holder>(Diff) {

    inner class Holder(private val binding: ItemPartnerClinicBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(clinic: Clinic) {
            binding.clinicName.text = clinic.name
            binding.clinicLocality.text = clinic.locality
            binding.root.setOnClickListener { onClick(clinic) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemPartnerClinicBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<Clinic>() {
        override fun areItemsTheSame(a: Clinic, b: Clinic) = a.id == b.id
        override fun areContentsTheSame(a: Clinic, b: Clinic) = a == b
    }
}
