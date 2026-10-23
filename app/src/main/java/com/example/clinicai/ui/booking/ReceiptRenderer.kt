package com.example.clinicai.ui.booking

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.example.clinicai.R
import com.example.clinicai.databinding.ItemKvRowBinding
import com.example.clinicai.domain.ReceiptRow
import com.example.clinicai.domain.ReceiptSection

/** Draws receipt sections as label/value rows, identical in content to the PDF. */
object ReceiptRenderer {

    fun renderSections(container: LinearLayout, sections: List<ReceiptSection>) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(container.context)
        sections.forEach { section ->
            container.addView(sectionTitle(container, section.title))
            section.rows.forEach { row -> container.addView(rowView(inflater, container, row)) }
        }
    }

    fun renderRows(container: LinearLayout, rows: List<ReceiptRow>) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(container.context)
        rows.forEach { container.addView(rowView(inflater, container, it)) }
    }

    private fun rowView(inflater: LayoutInflater, parent: ViewGroup, row: ReceiptRow) =
        ItemKvRowBinding.inflate(inflater, parent, false).also {
            it.rowLabel.text = row.label
            it.rowValue.text = row.value
            if (row.emphasized) it.rowValue.setTypeface(it.rowValue.typeface, android.graphics.Typeface.BOLD)
        }.root

    private fun sectionTitle(parent: ViewGroup, title: String) = TextView(parent.context).apply {
        setTextAppearance(R.style.TextAppearance_ClinicAI_Caption)
        text = title
        val pad = (12 * resources.displayMetrics.density).toInt()
        setPadding(0, pad, 0, (4 * resources.displayMetrics.density).toInt())
    }
}
