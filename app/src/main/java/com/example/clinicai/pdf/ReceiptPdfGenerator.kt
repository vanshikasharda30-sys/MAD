package com.example.clinicai.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.clinicai.domain.ReceiptDocument
import java.io.OutputStream

/**
 * Renders a [ReceiptDocument] to a paginated A4 PDF using Android's built-in PdfDocument.
 * All text comes from the document; nothing is hardcoded about any particular booking.
 */
class ReceiptPdfGenerator {

    private val ink = Color.parseColor("#12233D")
    private val inkMuted = Color.parseColor("#6B7686")
    private val teal = Color.parseColor("#1F6F6B")
    private val tealSoft = Color.parseColor("#E3EEED")
    private val line = Color.parseColor("#E4E0D6")
    private val amberSoft = Color.parseColor("#F6EEDC")
    private val amber = Color.parseColor("#7A5206")

    private val serifBold = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    private val sans = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    private val sansBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    private val mono = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

    fun write(document: ReceiptDocument, out: OutputStream): Int {
        val pdf = PdfDocument()
        try {
            val writer = PageWriter(pdf, document)
            writer.render()
            pdf.writeTo(out)
            return writer.pageCount
        } finally {
            pdf.close()
        }
    }

    private inner class PageWriter(private val pdf: PdfDocument, private val doc: ReceiptDocument) {
        var pageCount = 0
            private set
        private var page: PdfDocument.Page? = null
        private lateinit var canvas: Canvas
        private var y = 0f

        fun render() {
            newPage(first = true)
            drawTitleBlock()
            doc.sections.forEach { section ->
                val rows = section.rows.filter { it.label != "Booking ID" }.map { r -> Triple(r.label, r.value, r.emphasized) }
                if (rows.isNotEmpty()) drawSection(section.title, rows)
            }
            drawNotices()
            finishPage()
        }

        private fun newPage(first: Boolean = false) {
            pageCount++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageCount).create())
            canvas = page!!.canvas
            // Brand band
            canvas.drawRect(0f, 0f, PAGE_W.toFloat(), if (first) 74f else 52f, fill(ink))
            canvas.drawText("ClinicAI", MARGIN, if (first) 47f else 34f, text(Color.WHITE, if (first) 26f else 18f, serifBold))
            if (first) {
                canvas.drawText("Doctors, appointments and queues", PAGE_W - MARGIN - 160f, 45f, text(Color.parseColor("#B9C2D0"), 9f, sans))
            } else {
                canvas.drawText("Booking ${doc.bookingId}", PAGE_W - MARGIN - 190f, 33f, text(Color.parseColor("#B9C2D0"), 9f, mono))
            }
            y = if (first) 100f else 84f
        }

        private fun finishPage() {
            val p = page ?: return
            canvas.drawLine(MARGIN, PAGE_H - 44f, PAGE_W - MARGIN, PAGE_H - 44f, stroke(line, 0.8f))
            canvas.drawText(doc.footer, MARGIN, PAGE_H - 28f, text(inkMuted, 8f, sans))
            canvas.drawText("Page $pageCount", PAGE_W - MARGIN - 40f, PAGE_H - 28f, text(inkMuted, 8f, sans))
            pdf.finishPage(p)
            page = null
        }

        private fun ensureSpace(height: Float) {
            if (y + height > PAGE_H - BOTTOM_RESERVED) {
                finishPage()
                newPage()
            }
        }

        private fun drawTitleBlock() {
            canvas.drawText(doc.title, MARGIN, y, text(ink, 20f, serifBold))
            y += 20f
            canvas.drawText(doc.subtitle, MARGIN, y, text(inkMuted, 10.5f, sans))
            y += 18f

            // Status chip
            val chipPaint = text(teal, 9.5f, sansBold)
            val chipW = chipPaint.measureText(doc.statusLabel) + 22f
            canvas.drawRoundRect(RectF(MARGIN, y, MARGIN + chipW, y + 20f), 5f, 5f, fill(tealSoft))
            canvas.drawText(doc.statusLabel, MARGIN + 11f, y + 14f, chipPaint)
            y += 30f

            // Booking ID box
            canvas.drawRoundRect(RectF(MARGIN, y, PAGE_W - MARGIN, y + 46f), 8f, 8f, stroke(line, 1f))
            canvas.drawText("BOOKING ID", MARGIN + 14f, y + 17f, text(inkMuted, 8f, sansBold))
            canvas.drawText(doc.bookingId, MARGIN + 14f, y + 35f, text(ink, 14f, mono))
            y += 62f
        }

        private fun drawSection(title: String, rows: List<Triple<String, String, Boolean>>) {
            ensureSpace(44f)
            y += 4f
            canvas.drawText(title.uppercase(), MARGIN, y, text(inkMuted, 8.5f, sansBold).apply { letterSpacing = 0.1f })
            y += 6f
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, stroke(line, 0.8f))
            y += 13f
            for ((label, value, emphasized) in rows) {
                val valuePaint = TextPaint(text(ink, if (emphasized) 11.5f else 10.5f, if (emphasized) sansBold else sans))
                val layout = layout(value, valuePaint, VALUE_W.toInt())
                val rowH = maxOf(layout.height.toFloat(), 14f) + 4f
                ensureSpace(rowH)
                canvas.drawText(label, MARGIN, y + 10f, text(inkMuted, 9.5f, sans))
                canvas.save()
                canvas.translate(MARGIN + LABEL_W, y)
                layout.draw(canvas)
                canvas.restore()
                y += rowH
            }
            y += 8f
        }

        private fun drawNotices() {
            if (doc.notices.isEmpty()) return
            val paint = TextPaint(text(amber, 9.2f, sans))
            val layouts = doc.notices.map { layout(it, paint, (CONTENT_W - 28f).toInt()) }
            val gap = 5f
            val h = layouts.sumOf { it.height.toDouble() }.toFloat() + gap * (layouts.size - 1) + 20f
            ensureSpace(h + 4f)
            canvas.drawRoundRect(RectF(MARGIN, y, PAGE_W - MARGIN, y + h), 6f, 6f, fill(amberSoft))
            var top = y + 10f
            layouts.forEach { layout ->
                canvas.save()
                canvas.translate(MARGIN + 14f, top)
                layout.draw(canvas)
                canvas.restore()
                top += layout.height + gap
            }
            y += h + 4f
        }

        private fun layout(value: String, paint: TextPaint, width: Int): StaticLayout =
            StaticLayout.Builder.obtain(value, 0, value.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1f)
                .build()
    }

    private fun text(color: Int, size: Float, face: Typeface) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = face
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
    }

    companion object {
        const val PAGE_W = 595
        const val PAGE_H = 842
        private const val MARGIN = 44f
        private const val CONTENT_W = PAGE_W - 2 * MARGIN
        private const val LABEL_W = 150f
        private const val VALUE_W = CONTENT_W - LABEL_W
        private const val BOTTOM_RESERVED = 52f
    }
}
