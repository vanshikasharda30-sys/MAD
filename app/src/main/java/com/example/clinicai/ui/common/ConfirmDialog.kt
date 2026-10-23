package com.example.clinicai.ui.common

import android.content.Context
import androidx.appcompat.app.AlertDialog

/** Confirmation prompt for destructive actions (cancel appointment, sign out, deactivate doctor). */
fun Context.confirmDestructive(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setNegativeButton(cancelLabel, null)
        .setPositiveButton(confirmLabel) { _, _ -> onConfirm() }
        .show()
}
