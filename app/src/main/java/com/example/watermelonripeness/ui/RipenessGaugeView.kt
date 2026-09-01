package com.example.watermelonripeness.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
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
        color = Color.rgb(196, 63, 51)
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
        strokeCap = Paint.Cap.ROUND
    }
    private val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(196, 63, 51)
        style = Paint.Style.FILL
    }
    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(110, 45, 138, 87)
        style = Paint.Style.STROKE
        strokeWidth = 5f * density
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(56, 87, 68)
        textSize = 15f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }

    var value: Float = 0f
        private set

    private var pulseUntilMs: Long = 0L

    fun setGaugeValue(newValue: Float) {
        value = newValue.coerceIn(-1f, 1f)
        contentDescription = when {
            value < -0.333f -> "实时仪表盘：更靠近偏生一侧"
            value > 0.333f -> "实时仪表盘：更靠近偏熟一侧"
            else -> "实时仪表盘：更靠近适熟区域"
        }
        invalidate()
    }

    fun pulseTap() {
        pulseUntilMs = SystemClock.uptimeMillis() + TAP_PULSE_MS
        invalidate()
        postInvalidateDelayed(TAP_PULSE_MS)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (190f * density).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h * 0.70f
        val radius = min(w * 0.39f, h * 0.58f)
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        canvas.drawArc(rect, 180f, 180f, false, arcPaint)

        for (i in 0..10) {
            val angle = Math.toRadians(180.0 + i * 18.0)
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

        if (SystemClock.uptimeMillis() < pulseUntilMs) {
            canvas.drawCircle(cx, cy, 16f * density, pulsePaint)
        }

        val labelY = cy + 32f * density
        canvas.drawText("偏生", cx - radius * 0.82f, labelY, labelPaint)
        canvas.drawText("适熟", cx, labelY, labelPaint)
        canvas.drawText("偏熟", cx + radius * 0.82f, labelY, labelPaint)
    }

    companion object {
        private const val TAP_PULSE_MS = 180L
    }
}
