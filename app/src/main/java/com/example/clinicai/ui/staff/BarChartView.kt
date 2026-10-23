package com.example.clinicai.ui.staff

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.clinicai.R
import com.example.clinicai.data.model.DailyCount

/** Minimal bar chart for the 14-day appointment trend. */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var data: List<DailyCount> = emptyList()
    private val density = resources.displayMetrics.density
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.teal) }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.line) }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.ink_50)
        textSize = 10f * density
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.ink)
        textSize = 10f * density
        textAlign = Paint.Align.CENTER
    }

    fun setData(points: List<DailyCount>) {
        data = points
        contentDescription = points.joinToString(", ") { "${it.date}: ${it.count}" }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = (160 * density).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize(height, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        if (data.isEmpty()) return
        val labelH = 18f * density
        val valueH = 14f * density
        val chartBottom = height - labelH
        val chartTop = valueH
        val max = (data.maxOf { it.count }).coerceAtLeast(1)
        val slot = width.toFloat() / data.size
        val barW = slot * 0.6f

        data.forEachIndexed { i, point ->
            val cx = slot * i + slot / 2
            val h = (chartBottom - chartTop) * point.count / max
            val rect = RectF(cx - barW / 2, chartBottom - h.coerceAtLeast(2f * density), cx + barW / 2, chartBottom)
            canvas.drawRoundRect(rect, 3f * density, 3f * density, if (point.count == 0) emptyPaint else barPaint)
            if (point.count > 0) canvas.drawText(point.count.toString(), cx, rect.top - 3f * density, valuePaint)
            val day = point.date.takeLast(2).trimStart('0')
            canvas.drawText(day, cx, height - 4f * density, labelPaint)
        }
    }
}
