package com.morninglock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A scrollable "grain ruler" — a strip of tick marks where 1 grain = 1 minute. The user
 * scrolls (with fling + snap) and the grain under the center line is the selected value,
 * so any exact minute from [minValue]..[maxValue] is reachable. A haptic tick fires as
 * each grain crosses the center. Works horizontally or vertically (set via app:vertical).
 */
class GrainRuler @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var vertical = false
    var minValue = 1
    var maxValue = 720
    var onValueChanged: ((Int) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val spacingPx = 13f * density
    private val maxIndex get() = maxValue - minValue
    private val maxScroll get() = maxIndex * spacingPx

    private var _value = 30
    var value: Int
        get() = _value
        set(v) {
            _value = v.coerceIn(minValue, maxValue)
            scrollPx = (_value - minValue) * spacingPx
            invalidate()
        }

    /** Scroll position in pixels (0 == minValue centered). */
    private var scrollPx = (30 - 1) * 13f * density

    private val scroller = OverScroller(context)
    private var velocityTracker: VelocityTracker? = null
    private var lastTouch = 0f
    private val minFlingVel = ViewConfiguration.get(context).scaledMinimumFlingVelocity.toFloat()
    private val maxFlingVel = ViewConfiguration.get(context).scaledMaximumFlingVelocity.toFloat()

    private val minorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_hint); strokeWidth = 2f * density; strokeCap = Paint.Cap.ROUND
    }
    private val majorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_secondary); strokeWidth = 2.5f * density; strokeCap = Paint.Cap.ROUND
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.orange_primary); strokeWidth = 4f * density; strokeCap = Paint.Cap.ROUND
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_secondary); textSize = 11f * density; textAlign = Paint.Align.CENTER
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.GrainRuler)
            vertical = a.getBoolean(R.styleable.GrainRuler_rulerVertical, false)
            a.recycle()
        }
    }

    private fun updateSelected(haptic: Boolean) {
        val v = (scrollPx / spacingPx).roundToInt().coerceIn(0, maxIndex) + minValue
        if (v != _value) {
            _value = v
            if (haptic) tick()
            onValueChanged?.invoke(v)
        }
    }

    // ─── Drawing ───────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val length = if (vertical) height.toFloat() else width.toFloat()
        val cross  = if (vertical) width.toFloat() else height.toFloat()
        val center = length / 2f
        val crossMid = cross / 2f

        val firstIndex = ((scrollPx - center) / spacingPx).toInt() - 1
        val lastIndex  = ((scrollPx + center) / spacingPx).toInt() + 1

        for (i in firstIndex..lastIndex) {
            if (i < 0 || i > maxIndex) continue
            val v = i + minValue
            // distance from center along the scroll axis
            val pos = if (vertical) center + (scrollPx - i * spacingPx)
                      else          center + (i * spacingPx - scrollPx)

            val isHour = v % 60 == 0
            val isQuarter = v % 15 == 0
            val half = (if (isHour) 26f else if (isQuarter) 18f else 11f) * density
            val paint = if (isHour) majorPaint else minorPaint

            if (vertical) {
                canvas.drawLine(crossMid - half, pos, crossMid + half, pos, paint)
                if (isHour) canvas.drawText("${v / 60}h", crossMid + half + 16f * density, pos + 4f * density, labelPaint)
            } else {
                canvas.drawLine(pos, crossMid - half, pos, crossMid + half, paint)
                if (isHour) canvas.drawText("${v / 60}h", pos, crossMid + half + 16f * density, labelPaint)
            }
        }

        // Center indicator
        val indicatorHalf = 30f * density
        if (vertical) canvas.drawLine(crossMid - indicatorHalf, center, crossMid + indicatorHalf, center, centerPaint)
        else          canvas.drawLine(center, crossMid - indicatorHalf, center, crossMid + indicatorHalf, centerPaint)
    }

    // ─── Touch / scroll ──────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val axis = if (vertical) event.y else event.x
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                scroller.forceFinished(true)
                lastTouch = axis
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(event)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                // Dragging toward the start of the axis increases the value.
                val delta = lastTouch - axis
                lastTouch = axis
                scrollPx = (scrollPx + delta).coerceIn(0f, maxScroll)
                updateSelected(haptic = true)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000, maxFlingVel)
                val v = if (vertical) (velocityTracker?.yVelocity ?: 0f) else (velocityTracker?.xVelocity ?: 0f)
                velocityTracker?.recycle()
                velocityTracker = null
                val flingVel = -v   // toward axis start = positive scroll
                if (abs(flingVel) > minFlingVel) {
                    scroller.fling(scrollPx.toInt(), 0, flingVel.toInt(), 0, 0, maxScroll.toInt(), 0, 0)
                    postInvalidateOnAnimation()
                } else {
                    snapToNearest()
                }
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollPx = scroller.currX.toFloat().coerceIn(0f, maxScroll)
            updateSelected(haptic = true)
            invalidate()
            if (scroller.isFinished) snapToNearest() else postInvalidateOnAnimation()
        }
    }

    private fun snapToNearest() {
        val targetIndex = (scrollPx / spacingPx).roundToInt().coerceIn(0, maxIndex)
        val target = targetIndex * spacingPx
        if (abs(target - scrollPx) > 0.5f) {
            scroller.startScroll(scrollPx.toInt(), 0, (target - scrollPx).toInt(), 0, 220)
            postInvalidateOnAnimation()
        } else {
            scrollPx = target
        }
        updateSelected(haptic = false)
    }

    override fun performClick(): Boolean { super.performClick(); return true }

    private fun tick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            vibrator?.vibrate(VibrationEffect.createOneShot(12, 60))
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val thickness = (96 * density).toInt()
        if (vertical) {
            val w = resolveSize(thickness, widthMeasureSpec)
            val h = resolveSize((360 * density).toInt(), heightMeasureSpec)
            setMeasuredDimension(w, h)
        } else {
            val w = resolveSize((320 * density).toInt(), widthMeasureSpec)
            val h = resolveSize(thickness, heightMeasureSpec)
            setMeasuredDimension(w, h)
        }
    }
}
