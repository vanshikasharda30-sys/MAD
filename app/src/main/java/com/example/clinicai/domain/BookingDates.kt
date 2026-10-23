package com.example.clinicai.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DayItem(
    /** `yyyy-MM-dd` in the device's local calendar. */
    val iso: String,
    /** e.g. `MON`. */
    val weekday: String,
    val dayOfMonth: Int,
    val isToday: Boolean,
)

object BookingDates {
    fun next(days: Int, from: Calendar = Calendar.getInstance(), locale: Locale = Locale.ENGLISH): List<DayItem> {
        val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = from.timeZone }
        val weekday = SimpleDateFormat("EEE", locale).apply { timeZone = from.timeZone }
        return (0 until days).map { offset ->
            val cal = (from.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
            DayItem(
                iso = iso.format(cal.time),
                weekday = weekday.format(cal.time).uppercase(locale),
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                isToday = offset == 0,
            )
        }
    }
}
