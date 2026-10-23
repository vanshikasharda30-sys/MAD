package com.example.clinicai.ui.common

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Collects [flow] only while the fragment's view is at least STARTED. */
fun <T> Fragment.collectFlow(flow: Flow<T>, collector: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { collector(it) }
        }
    }
}

/** Runs [block] repeatedly (with [intervalMs] between runs) while the view is STARTED, e.g. queue polling. */
fun Fragment.pollWhileStarted(intervalMs: Long, block: suspend () -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                block()
                kotlinx.coroutines.delay(intervalMs)
            }
        }
    }
}

@Suppress("unused")
private fun CoroutineScope.unused() = Unit
