package com.example.clinicai.ui.staff

import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import com.example.clinicai.R
import com.example.clinicai.data.model.CreateDoctorRequest
import com.example.clinicai.data.model.Specialization
import com.example.clinicai.databinding.DialogAddDoctorBinding
import com.example.clinicai.ui.common.setErrorIf
import com.example.clinicai.ui.common.text
import com.example.clinicai.util.Validators

/** Admin-only form that posts to `POST /api/doctors`. Every field is validated before submitting. */
object AddDoctorDialog {

    fun show(context: Context, specializations: List<Specialization>, onSubmit: (CreateDoctorRequest) -> Unit) {
        val binding = DialogAddDoctorBinding.inflate(LayoutInflater.from(context))
        var chosen: Specialization? = null
        binding.specialtyDropdown.setAdapter(
            ArrayAdapter(context, android.R.layout.simple_list_item_1, specializations.map { it.name }),
        )
        binding.specialtyDropdown.setOnItemClickListener { _, _, position, _ ->
            chosen = specializations.getOrNull(position)
            binding.specialtyLayout.setErrorIf(null)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.staff_add_doctor)
            .setView(binding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val required = context.getString(R.string.validation_required)
                val number = context.getString(R.string.validation_number)
                var valid = true

                val name = binding.nameLayout.text.trim()
                val qualification = binding.qualificationLayout.text.trim()
                val experience = binding.experienceLayout.text.trim()
                val fee = binding.feeLayout.text.trim()
                val slot = binding.slotLayout.text.trim()

                binding.nameLayout.setErrorIf(if (name.isEmpty()) required else null).also { if (name.isEmpty()) valid = false }
                binding.qualificationLayout.setErrorIf(if (qualification.isEmpty()) required else null).also { if (qualification.isEmpty()) valid = false }
                if (chosen == null) { binding.specialtyLayout.setErrorIf(required); valid = false }
                if (!Validators.isNonNegativeInt(experience)) { binding.experienceLayout.setErrorIf(number); valid = false } else binding.experienceLayout.setErrorIf(null)
                if (!Validators.isNonNegativeInt(fee)) { binding.feeLayout.setErrorIf(number); valid = false } else binding.feeLayout.setErrorIf(null)
                if (!Validators.isPositiveInt(slot)) { binding.slotLayout.setErrorIf(number); valid = false } else binding.slotLayout.setErrorIf(null)

                if (valid) {
                    onSubmit(
                        CreateDoctorRequest(
                            name = name,
                            qualification = qualification,
                            specializationId = chosen!!.id,
                            experienceYears = experience.toInt(),
                            consultationFee = fee.toInt(),
                            slotDurationMinutes = slot.toInt(),
                            avgConsultationMinutes = slot.toInt(),
                        ),
                    )
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }
}
