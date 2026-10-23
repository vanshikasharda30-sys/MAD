package com.example.clinicai.testing

import android.view.View
import androidx.test.espresso.matcher.BoundedMatcher
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.Description
import org.hamcrest.Matcher

/** Matches a TextInputLayout whose *error* (not helper) text satisfies [textMatcher]. */
fun hasInputError(textMatcher: Matcher<String>): Matcher<View> =
    object : BoundedMatcher<View, TextInputLayout>(TextInputLayout::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("TextInputLayout with error ").appendDescriptionOf(textMatcher)
        }

        override fun matchesSafely(view: TextInputLayout): Boolean =
            view.error?.toString()?.let { textMatcher.matches(it) } ?: false
    }
