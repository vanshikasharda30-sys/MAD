package com.example.clinicai.ui.staff

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.clinicai.data.model.Appointment
import com.example.clinicai.databinding.ItemStaffAppointmentBinding
import com.example.clinicai.ui.common.show
import com.example.clinicai.util.DateFormats

class StaffAppointmentAdapter : ListAdapter<Appointment, StaffAppointmentAdapter.Holder>(Diff) {

    class Holder(private val binding: ItemStaffAppointmentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(a: Appointment) {
            binding.patientName.text = a.patient?.name ?: "—"
            binding.doctorName.text = a.doctor?.name.orEmpty()
            binding.whenText.text = listOfNotNull(
                DateFormats.shortDate(a.dateOnly),
                a.startTime?.let(DateFormats::time12h),
            ).joinToString(" · ")
            binding.notes.show(!a.notes.isNullOrBlank())
            binding.notes.text = a.notes.orEmpty()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemStaffAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<Appointment>() {
        override fun areItemsTheSame(a: Appointment, b: Appointment) = a.id == b.id
        override fun areContentsTheSame(a: Appointment, b: Appointment) = a == b
    }
}
