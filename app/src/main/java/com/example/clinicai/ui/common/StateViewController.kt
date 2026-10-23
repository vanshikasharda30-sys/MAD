package com.example.clinicai.ui.common

import android.content.Context
import android.view.View
import com.example.clinicai.R
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.databinding.LayoutStateViewBinding

/** Drives the shared loading / empty / error block (`layout_state_view`). */
class StateViewController(
    private val binding: LayoutStateViewBinding,
    private val context: Context,
    private val onRetry: () -> Unit,
) {
    init {
        binding.stateRetry.setOnClickListener { onRetry() }
    }

    fun showLoading() = show(loading = true)

    fun showEmpty(title: String, body: String? = null) {
        binding.stateEmptyTitle.text = title
        binding.stateEmptyBody.text = body.orEmpty()
        binding.stateEmptyBody.visibility = if (body.isNullOrBlank()) View.GONE else View.VISIBLE
        show(empty = true)
    }

    fun showError(error: AppError) = showError(error.toMessage(context))

    fun showError(message: String) {
        binding.stateErrorMessage.text = message
        show(error = true)
    }

    fun hide() = show()

    private fun show(loading: Boolean = false, empty: Boolean = false, error: Boolean = false) {
        binding.stateProgress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.stateEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.stateError.visibility = if (error) View.VISIBLE else View.GONE
        binding.root.visibility = if (loading || empty || error) View.VISIBLE else View.GONE
    }

    companion object {
        fun loadingText(context: Context) = context.getString(R.string.loading)
    }
}
