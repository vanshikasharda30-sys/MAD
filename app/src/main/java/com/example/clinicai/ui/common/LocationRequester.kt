package com.example.clinicai.ui.common

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.clinicai.R
import com.example.clinicai.data.location.LocationRepository
import kotlinx.coroutines.launch

/**
 * Asks for the user's approximate location at the moment they ask for distances, explaining why
 * first. Create it as a property of the fragment so the permission launcher is registered in time.
 */
class LocationRequester(private val fragment: Fragment, private val repositoryProvider: () -> LocationRepository) {

    private val repository get() = repositoryProvider()

    // Both are requested together so Android's prompt offers the Precise / Approximate choice.
    private val launcher = fragment.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.any { it }) fragment.lifecycleScope.launch { repository.refresh(force = true) } else repository.markPermissionDenied()
    }

    /** Fetches a position, requesting permission (after a short explanation) if it isn't granted yet. */
    fun request() {
        if (repository.permissionGranted()) {
            fragment.lifecycleScope.launch { repository.refresh() }
            return
        }
        AlertDialog.Builder(fragment.requireContext())
            .setTitle(R.string.location_why_title)
            .setMessage(R.string.location_why_body)
            .setNegativeButton(R.string.location_not_now) { _, _ -> repository.markPermissionDenied() }
            .setPositiveButton(R.string.location_allow) { _, _ -> launcher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) }
            .show()
    }

    /** For the "turn it on in Settings" case, where the system will no longer show the prompt. */
    fun openAppSettings() {
        fragment.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", fragment.requireContext().packageName, null)),
        )
    }
}
