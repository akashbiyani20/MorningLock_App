package com.morninglock

import android.graphics.Typeface
import android.os.*
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextSwitcher
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LockActivity : AppCompatActivity() {

    private lateinit var clockWrap: View
    private lateinit var tsHours: TextSwitcher
    private lateinit var tsMinutes: TextSwitcher
    private lateinit var tsSeconds: TextSwitcher
    private lateinit var completionOverlay: View
    private lateinit var tvCompletionMsg: TextView

    private val handler = Handler(Looper.getMainLooper())

    private var lastH = ""
    private var lastM = ""
    private var lastS = ""
    private var lastSecLeft = -1
    private var isFinished = false
    private var isDimmed = false

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }
    }

    companion object {
        const val AUTO_DIM_DELAY_MS = 10_000L
        const val DIM_BRIGHTNESS = 0.01f
        const val DIM_ALPHA = 0.4f
        const val TICK_MS = 250L
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (isFinished) return
            val remaining = LockService.lockEndTime - System.currentTimeMillis()

            if (remaining <= 0) {
                showCompletion()
                return
            }
            if (!LockService.isRunning) {   // ended early / cancelled
                finish()
                return
            }

            val secLeft = (remaining / 1000).toInt()
            render(secLeft)

            if (secLeft != lastSecLeft) {
                lastSecLeft = secLeft
                if (secLeft in 1..3) vibrateTick()   // 3 … 2 … 1 buzz
            }
            handler.postDelayed(this, TICK_MS)
        }
    }

    private val autoDimRunnable = Runnable { enterDim() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(R.layout.activity_lock)

        clockWrap         = findViewById(R.id.clockWrap)
        tsHours           = findViewById(R.id.tsHours)
        tsMinutes         = findViewById(R.id.tsMinutes)
        tsSeconds         = findViewById(R.id.tsSeconds)
        completionOverlay = findViewById(R.id.completionOverlay)
        tvCompletionMsg   = findViewById(R.id.tvCompletionMsg)
        findViewById<Button>(R.id.btnDone).setOnClickListener { finish() }

        val softWhite = 0xFFECECF1.toInt()   // calm, not harsh
        val orange    = getColor(R.color.orange_primary)
        setupSwitcher(tsHours, softWhite)
        setupSwitcher(tsMinutes, softWhite)
        setupSwitcher(tsSeconds, orange)

        // First paint without animation.
        val remaining = (LockService.lockEndTime - System.currentTimeMillis()).coerceAtLeast(0)
        val s = (remaining / 1000).toInt()
        lastH = "%02d".format(s / 3600)
        lastM = "%02d".format((s % 3600) / 60)
        lastS = "%02d".format(s % 60)
        tsHours.setCurrentText(lastH)
        tsMinutes.setCurrentText(lastM)
        tsSeconds.setCurrentText(lastS)

        handler.post(tickRunnable)
        scheduleAutoDim()
    }

    private fun setupSwitcher(ts: TextSwitcher, color: Int) {
        ts.setFactory {
            TextView(this).apply {
                gravity = Gravity.CENTER
                setTextColor(color)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.flip_digit_size))
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                includeFontPadding = false
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
        }
        ts.inAnimation = AnimationUtils.loadAnimation(this, R.anim.flip_in)
        ts.outAnimation = AnimationUtils.loadAnimation(this, R.anim.flip_out)
    }

    /** Update each card; only the ones that changed animate (so seconds roll every tick). */
    private fun render(totalSec: Int) {
        val s = totalSec.coerceAtLeast(0)
        val hh = "%02d".format(s / 3600)
        val mm = "%02d".format((s % 3600) / 60)
        val ss = "%02d".format(s % 60)
        if (hh != lastH) { tsHours.setText(hh);   lastH = hh }
        if (mm != lastM) { tsMinutes.setText(mm); lastM = mm }
        if (ss != lastS) { tsSeconds.setText(ss); lastS = ss }
    }

    private fun vibrateTick() {
        vibrator?.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    // ─── Completion ────────────────────────────────────────────────────────────

    private fun showCompletion() {
        if (isFinished) return
        isFinished = true
        handler.removeCallbacks(tickRunnable)
        handler.removeCallbacks(autoDimRunnable)

        window.attributes = window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        isDimmed = false

        clockWrap.visibility = View.GONE
        tvCompletionMsg.text =
            "You stayed off your phone for ${formatDuration(LockService.lockTotalMinutes)}.\nNice work. 💪"
        completionOverlay.visibility = View.VISIBLE

        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120, 80, 350), -1))
    }

    private fun formatDuration(minutes: Int): String =
        if (minutes < 60) "$minutes min"
        else {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0) "${h}h" else "${h}h ${m}m"
        }

    // ─── Always-on dim (near-black, faint clock) ──────────────────────────────

    private fun scheduleAutoDim() {
        handler.removeCallbacks(autoDimRunnable)
        handler.postDelayed(autoDimRunnable, AUTO_DIM_DELAY_MS)
    }

    private fun enterDim() {
        if (isDimmed || isFinished) return
        isDimmed = true
        clockWrap.alpha = DIM_ALPHA
        window.attributes = window.attributes.apply { screenBrightness = DIM_BRIGHTNESS }
    }

    private fun exitDim() {
        isDimmed = false
        clockWrap.alpha = 1f
        window.attributes = window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        scheduleAutoDim()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN && !isFinished) {
            if (isDimmed) { exitDim(); return true }
            scheduleAutoDim()
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickRunnable)
        handler.removeCallbacks(autoDimRunnable)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() { /* locked — back button does nothing */ }
}
