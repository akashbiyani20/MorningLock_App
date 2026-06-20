package com.morninglock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

/**
 * A modern vertical "grain" slider — a stack of horizontal pebbles that fill from the
 * bottom up as the user drags. Spans 1 minute to 12 hours with finer steps at the low
 * end, and fires a haptic tick as each grain is crossed (mirroring the alarm screen's
 * lock-duration slider).
 */
class GrainSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    /** Selectable minute stops, fine near the bottom and coarser toward 12 h. */
    private val values: IntArray = buildValues()

    /** Index of the currently selected stop (0-based). */
    private var level = values.indexOf(30).coerceAtLeast(0)

    /** Selected duration in minutes. */
    var minutes: Int
        get() = values[level]
        set(value) {
            // Snap to the closest available stop.
            var best = 0
            var bestDiff = Int.MAX_VALUE
            for (i in values.indices) {
                val d = kotlin.math.abs(values[i] - value)
                if (d < bestDiff) { bestDiff = d; best = i }
            }
            level = best
            invalidate()
        }

    var onMinutesChanged: ((Int) -> Unit)? = null

    private val filledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF6B35.toInt() }
    private val trackPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33808080 }
    private val topPaint    = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF8B5C.toInt() }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val rect = RectF()

    private fun buildValues(): IntArray {
        val list = ArrayList<Int>()
        var m = 1
        while (m <= 15)  { list.add(m); m += 1 }    // 1..15  by 1
        m = 20
        while (m <= 60)  { list.add(m); m += 5 }    // 20..60 by 5
        m = 75
        while (m <= 120) { list.add(m); m += 15 }   // 75..120 by 15
        m = 150
        while (m <= 720) { list.add(m); m += 30 }   // 150..720 (12h) by 30
        return list.toIntArray()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val n = values.size
        val h = height.toFloat()
        val w = width.toFloat()
        val slot = h / n
        val grainH = slot * 0.62f
        val grainW = w * 0.7f
        val left = (w - grainW) / 2f
        val radius = grainH / 2f

        for (i in 0 until n) {
            val centerY = h - (i + 0.5f) * slot   // i = 0 is the bottom grain
            rect.set(left, centerY - grainH / 2f, left + grainW, centerY + grainH / 2f)
            val paint = when {
                i == level -> topPaint
                i < level  -> filledPaint
                else       -> trackPaint
            }
            canvas.drawRoundRect(rect, radius, radius, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val y = event.y.coerceIn(0f, height.toFloat())
                val slot = height.toFloat() / values.size
                val newLevel = ((height - y) / slot).toInt().coerceIn(0, values.size - 1)
                if (newLevel != level) {
                    level = newLevel
                    tick()
                    invalidate()
                    onMinutesChanged?.invoke(minutes)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun tick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            vibrator?.vibrate(VibrationEffect.createOneShot(15, 80))
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize((90 * resources.displayMetrics.density).toInt(), widthMeasureSpec)
        val desiredH = (380 * resources.displayMetrics.density).toInt()
        val h = resolveSize(max(desiredH, suggestedMinimumHeight), heightMeasureSpec)
        setMeasuredDimension(w, h)
    }
}
