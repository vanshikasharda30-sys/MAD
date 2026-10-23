package com.example.clinicai.ui.checkin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.clinicai.R
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.databinding.ItemCheckinDoctorBinding

class CheckinDoctorAdapter(
    private val onJoin: (Doctor) -> Unit,
) : ListAdapter<Doctor, CheckinDoctorAdapter.Holder>(Diff) {

    /** Id of the doctor whose join request is in flight; their button shows progress, all others are disabled. */
    var joiningDoctorId: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class Holder(private val binding: ItemCheckinDoctorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(doctor: Doctor) {
            binding.doctorInitials.text = doctor.initials
            binding.doctorName.text = doctor.name
            binding.doctorSpecialty.text = doctor.specializationName
            val joining = joiningDoctorId == doctor.id
            binding.joinButton.isEnabled = joiningDoctorId == null
            binding.joinButton.setText(if (joining) R.string.checkin_joining else R.string.checkin_join)
            binding.joinButton.setOnClickListener { onJoin(doctor) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemCheckinDoctorBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<Doctor>() {
        override fun areItemsTheSame(a: Doctor, b: Doctor) = a.id == b.id
        override fun areContentsTheSame(a: Doctor, b: Doctor) = a == b
    }
}
