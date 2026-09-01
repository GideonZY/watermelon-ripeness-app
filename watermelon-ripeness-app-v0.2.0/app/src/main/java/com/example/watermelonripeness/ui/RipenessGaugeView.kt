package com.example.watermelonripeness.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class RipenessGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(72, 101, 79)
        style = Paint.Style.STROKE
        strokeWidth = 10f * density
        strokeCap = Paint.Cap.ROUND
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(96, 115, 102)
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(179, 38, 30)
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
        strokeCap = Paint.Cap.ROUND
    }
    private val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(179, 38, 30)
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(56, 87, 68)
        textSize = 16f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }

    var value: Float = 0f
        private set

    fun setGaugeValue(newValue: Float) {
        value = newValue.coerceIn(-1f, 1f)
        contentDescription = when {
            value < -0.333f -> "成熟度仪表盘：偏生"
            value > 0.333f -> "成熟度仪表盘：偏熟"
            else -> "成熟度仪表盘：适熟"
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (210f * density).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h * 0.72f
        val radius = min(w * 0.39f, h * 0.58f)
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        canvas.drawArc(rect, 180f, 180f, false, arcPaint)

        for (i in 0..10) {
            val angle = Math.toRadians((180.0 + i * 18.0))
            val outerX = cx + radius * cos(angle).toFloat()
            val outerY = cy + radius * sin(angle).toFloat()
            val innerRadius = radius - if (i == 5) 18f * density else 11f * density
            val innerX = cx + innerRadius * cos(angle).toFloat()
            val innerY = cy + innerRadius * sin(angle).toFloat()
            canvas.drawLine(innerX, innerY, outerX, outerY, tickPaint)
        }

        val needleAngle = Math.toRadians((270.0 + value * 90.0).toDouble())
        val needleLength = radius - 24f * density
        val needleX = cx + needleLength * cos(needleAngle).toFloat()
        val needleY = cy + needleLength * sin(needleAngle).toFloat()
        canvas.drawLine(cx, cy, needleX, needleY, needlePaint)
        canvas.drawCircle(cx, cy, 7f * density, hubPaint)

        val labelY = cy + 34f * density
        canvas.drawText("生", cx - radius * 0.82f, labelY, labelPaint)
        canvas.drawText("熟", cx, labelY, labelPaint)
        canvas.drawText("过熟", cx + radius * 0.82f, labelY, labelPaint)
    }
}
