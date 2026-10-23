package com.example.clinicai.ui.staff

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.clinicai.R
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.databinding.ItemStaffDoctorBinding
import com.example.clinicai.ui.common.show
import com.example.clinicai.util.DateFormats

class StaffDoctorAdapter(
    private val canDeactivate: Boolean,
    private val onDeactivate: (Doctor) -> Unit,
) : ListAdapter<Doctor, StaffDoctorAdapter.Holder>(Diff) {

    inner class Holder(private val binding: ItemStaffDoctorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(d: Doctor) {
            val context = binding.root.context
            binding.doctorInitials.text = d.initials
            binding.doctorName.text = d.name
            binding.doctorSpecialty.text = d.specializationName
            binding.doctorMeta.text = "₹${DateFormats.rupees(d.consultationFee)}  ·  " +
                if (d.hasReviews) "★ %.1f".format(d.avgRating) else context.getString(R.string.doctor_new)
            binding.deactivateButton.show(canDeactivate)
            binding.deactivateButton.setOnClickListener { onDeactivate(d) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemStaffDoctorBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<Doctor>() {
        override fun areItemsTheSame(a: Doctor, b: Doctor) = a.id == b.id
        override fun areContentsTheSame(a: Doctor, b: Doctor) = a == b
    }
}
