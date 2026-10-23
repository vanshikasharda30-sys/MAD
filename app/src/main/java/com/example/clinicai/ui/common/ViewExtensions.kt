package com.example.clinicai.ui.common

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout

fun View.show(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun Activity.hideKeyboard() {
    currentFocus?.hideKeyboard()
}

fun View.snack(message: String, length: Int = Snackbar.LENGTH_LONG) {
    Snackbar.make(this, message, length).show()
}

/** A snackbar with one action button, e.g. "Try again" or "Settings". */
fun View.snack(message: String, actionLabel: String, onAction: () -> Unit) {
    Snackbar.make(this, message, Snackbar.LENGTH_INDEFINITE).setAction(actionLabel) { onAction() }.show()
}

/** Text of the EditText inside this TextInputLayout (empty string if none). */
val TextInputLayout.text: String
    get() = editText?.text?.toString().orEmpty()

/** Sets or clears the inline error on a field. */
fun TextInputLayout.setErrorIf(message: String?) {
    error = message
    isErrorEnabled = message != null
}

fun EditText.clearErrorOnChange(layout: TextInputLayout) {
    addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (layout.error != null) layout.setErrorIf(null)
        }
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    })
}
