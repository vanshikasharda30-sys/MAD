package com.example.clinicai.ui.appointments

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.example.clinicai.R
import com.example.clinicai.databinding.DialogReviewBinding

/** Star rating + optional comment, matching the website's "Rate your visit" prompt. */
object ReviewDialog {
    fun show(context: Context, onSubmit: (rating: Int, comment: String?) -> Unit) {
        val binding = DialogReviewBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.review_title)
            .setView(binding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_submit, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val rating = binding.ratingBar.rating.toInt()
                if (rating < 1) {
                    binding.ratingError.visibility = android.view.View.VISIBLE
                } else {
                    onSubmit(rating, binding.commentInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() })
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }
}
