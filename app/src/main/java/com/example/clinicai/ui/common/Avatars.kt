package com.example.clinicai.ui.common

import android.widget.TextView
import com.example.clinicai.data.model.Doctor

/** Initials avatar, as the website shows for doctors without a photo. */
fun TextView.bindInitials(doctor: Doctor) {
    text = doctor.initials
}
