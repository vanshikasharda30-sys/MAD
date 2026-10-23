package com.example.clinicai.ui.common

import com.example.clinicai.data.remote.AppError

/** What a data-driven screen is currently showing. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Empty(val reason: String? = null) : UiState<Nothing>
    data class Error(val error: AppError) : UiState<Nothing>
}
