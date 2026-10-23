package com.example.clinicai.ui.doctors

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.clinicai.R
import com.example.clinicai.data.location.LocationStatus
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.domain.DistanceResolver
import com.example.clinicai.domain.DoctorDistance
import com.example.clinicai.domain.GeoDistance
import com.example.clinicai.databinding.ItemDoctorBinding
import com.example.clinicai.ui.common.show
import com.example.clinicai.util.DateFormats

class DoctorAdapter(
    private val onClick: (Doctor) -> Unit,
) : ListAdapter<Doctor, DoctorAdapter.Holder>(Diff) {

    /** Drives the distance line on each card; see [DistanceResolver]. */
    var location: LocationStatus = LocationStatus.NotRequested
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }

    inner class Holder(private val binding: ItemDoctorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(doctor: Doctor) {
            val context = binding.root.context
            binding.doctorInitials.text = doctor.initials
            binding.doctorName.text = doctor.name
            binding.doctorSubtitle.text = listOf(doctor.specializationName, doctor.clinic?.area.orEmpty())
                .filter { it.isNotBlank() }.joinToString(" · ")
            binding.doctorClinic.text = doctor.clinicName
            binding.doctorMeta.text = buildString {
                append(if (doctor.hasReviews) "★ %.1f (%d)".format(doctor.avgRating, doctor.reviewCount) else context.getString(R.string.doctor_new))
                append("  ·  ₹").append(DateFormats.rupees(doctor.consultationFee))
                append("  ·  ").append(context.getString(R.string.doctor_experience, doctor.experienceYears))
            }
            when (val distance = DistanceResolver.resolve(doctor.clinic, location)) {
                DoctorDistance.Hidden -> binding.doctorDistance.show(false)
                is DoctorDistance.Known -> {
                    binding.doctorDistance.show(true)
                    binding.doctorDistance.text = context.getString(if (distance.approximate) R.string.distance_about else R.string.distance_from_you, GeoDistance.format(distance.km))
                    binding.doctorDistance.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.teal))
                }
                DoctorDistance.Unavailable -> {
                    binding.doctorDistance.show(true)
                    binding.doctorDistance.text = context.getString(R.string.distance_unavailable)
                    binding.doctorDistance.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.ink_50))
                }
            }
            binding.root.setOnClickListener { onClick(doctor) }
            binding.root.contentDescription = "${doctor.name}, ${doctor.specializationName}, ${doctor.clinicName}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemDoctorBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<Doctor>() {
        override fun areItemsTheSame(a: Doctor, b: Doctor) = a.id == b.id
        override fun areContentsTheSame(a: Doctor, b: Doctor) = a == b
    }
}
