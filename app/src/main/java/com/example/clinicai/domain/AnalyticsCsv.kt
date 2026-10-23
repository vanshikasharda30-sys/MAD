package com.example.clinicai.domain

import com.example.clinicai.data.model.DoctorStat

/** The doctor ledger as CSV — same columns the website's "Export" produces. */
object AnalyticsCsv {

    fun build(stats: List<DoctorStat>): String {
        val header = "Doctor,Appointments,No-shows,Avg Rating,Reviews\n"
        val rows = stats.joinToString("\n") { d ->
            listOf(
                escape(d.name),
                d.appointmentCount.toString(),
                d.noShowCount.toString(),
                if (d.reviewCount > 0) "%.1f".format(java.util.Locale.US, d.avgRating) else "",
                d.reviewCount.toString(),
            ).joinToString(",")
        }
        return header + rows
    }

    /** Quote a field if it contains a comma, quote or newline. */
    internal fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' }) "\"" + field.replace("\"", "\"\"") + "\"" else field
}
