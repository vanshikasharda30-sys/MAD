package com.example.clinicai.util

/** One-shot UI event (navigation, snackbar) that must not replay after rotation. */
class Event<out T>(private val content: T) {
    private var handled = false

    fun getContentIfNotHandled(): T? =
        if (handled) null else {
            handled = true
            content
        }

    fun peek(): T = content
}
