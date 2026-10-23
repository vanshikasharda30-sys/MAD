package com.example.clinicai.ui.common

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.clinicai.R
import com.example.clinicai.data.model.Clinic
import com.example.clinicai.domain.ContactValidator
import com.example.clinicai.ui.auth.AuthActivity
import com.example.clinicai.ui.staff.StaffActivity
import com.google.android.material.appbar.MaterialToolbar

/** Back arrow on a detail screen's toolbar. */
fun Fragment.setupBackToolbar(toolbar: MaterialToolbar) {
    toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
}

fun Fragment.openDoctor(doctorId: String) {
    findNavController().navigate(R.id.doctorDetailFragment, bundleOf("doctorId" to doctorId))
}

fun Fragment.openAppointment(appointmentId: String) {
    findNavController().navigate(R.id.appointmentDetailFragment, bundleOf("appointmentId" to appointmentId))
}

fun Fragment.openCheckin(token: String? = null) {
    findNavController().navigate(R.id.checkinFragment, bundleOf("token" to token))
}

fun Fragment.openFindDoctors(specializationId: String? = null) {
    findNavController().navigate(R.id.findDoctorFragment, bundleOf("specializationId" to specializationId))
}

fun Fragment.openStaffDashboard() {
    startActivity(Intent(requireContext(), StaffActivity::class.java))
}

fun authIntent(context: Context): Intent = Intent(context, AuthActivity::class.java)

/** Where a clinic is, preferring its real coordinates and falling back to its street address. */
private fun mapQuery(clinic: Clinic): String =
    if (clinic.hasCoordinates) "${clinic.latitude},${clinic.longitude}" else clinic.fullAddress

/** Shows the clinic's pin (named) in a maps app, or in the browser if none is installed. */
fun Fragment.openClinicInMaps(clinic: Clinic) {
    val label = Uri.encode(clinic.name)
    val geo = if (clinic.hasCoordinates) {
        Uri.parse("geo:${clinic.latitude},${clinic.longitude}?q=${clinic.latitude},${clinic.longitude}($label)")
    } else {
        Uri.parse("geo:0,0?q=${Uri.encode(clinic.fullAddress)}")
    }
    startFirstAvailable(
        Intent(Intent.ACTION_VIEW, geo),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(mapQuery(clinic))}")),
    )
}

/** Turn-by-turn directions to the clinic. */
fun Fragment.openDirections(clinic: Clinic) {
    val destination = Uri.encode(mapQuery(clinic))
    startFirstAvailable(
        Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$destination")),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destination")),
    )
}

/** Opens the dialer with a validated number (the user still has to press call). */
fun Fragment.dial(phone: ContactValidator.Phone) {
    startFirstAvailable(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.dial}")))
}

/** Opens the e-mail app addressed to a validated address. */
fun Fragment.composeEmail(address: String, subject: String) {
    startFirstAvailable(
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")).putExtra(Intent.EXTRA_SUBJECT, subject),
    )
}

/** Tries each intent in order; tells the user if nothing on the device can handle any of them. */
private fun Fragment.startFirstAvailable(vararg intents: Intent) {
    for (intent in intents) {
        try {
            startActivity(intent)
            return
        } catch (e: android.content.ActivityNotFoundException) {
            // try the next option
        }
    }
    view?.snack(getString(R.string.error_no_app_for_action))
}

fun Activity.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
