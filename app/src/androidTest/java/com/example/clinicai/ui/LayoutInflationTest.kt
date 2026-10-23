package com.example.clinicai.ui

import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clinicai.R
import com.example.clinicai.ui.main.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every layout shipped in the app must inflate inside a real activity (catches broken attributes,
 * styles, missing ids and custom views). The three activity shells that host a NavHostFragment are
 * excluded here because the other tests launch those activities for real.
 */
@RunWith(AndroidJUnit4::class)
class LayoutInflationTest {

    private val activityShells = setOf("activity_main", "activity_auth", "activity_staff")

    @Test fun everyLayoutInflates() {
        val layouts = R.layout::class.java.fields
            .filter { it.type == Int::class.javaPrimitiveType }
            .map { it.name to it.getInt(null) }
            .filter { (name, _) ->
                name.startsWith("activity_") || name.startsWith("fragment_") || name.startsWith("item_") ||
                    name.startsWith("dialog_") || name.startsWith("layout_") || name.startsWith("view_") || name.startsWith("header_")
            }
            .filterNot { (name, _) -> name in activityShells }

        assertTrue("expected the app's layouts to be discovered, found ${layouts.size}", layouts.size >= 40)

        val failures = mutableListOf<String>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                layouts.forEach { (name, id) ->
                    try {
                        LayoutInflater.from(activity).inflate(id, FrameLayout(activity), false)
                    } catch (e: Exception) {
                        var cause: Throwable? = e
                        while (cause?.cause != null) cause = cause.cause
                        failures += "$name: ${cause?.javaClass?.simpleName}: ${cause?.message?.take(200)}"
                    }
                }
            }
        }
        assertTrue("layouts failed to inflate:\n${failures.joinToString("\n")}", failures.isEmpty())
    }
}
