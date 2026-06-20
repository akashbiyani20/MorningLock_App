package com.morninglock

import android.os.*
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LockActivity : AppCompatActivity() {

    private lateinit var lockRoot: View
    private lateinit var tvTimer: TextView
    private lateinit var completionOverlay: View
    private lateinit var tvCompletionMsg: TextView

    private val handler = Handler(Looper.getMainLooper())

    // Bounce state
    private var posX = 0f
    private var posY = 0f
    private var velX = 0f
    private var velY = 0f
    private var colorIndex = 0

    // Countdown state
    private var lastSecShown = -1
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
        const val AUTO_DIM_DELAY_MS = 12_000L
        const val DIM_BRIGHTNESS = 0.02f
        const val TICK_MS = 250L
        const val FRAME_MS = 16L

        // Colors the timer cycles through on each wall bounce (the "fun" factor).
        val BOUNCE_COLORS = intArrayOf(
            0xFFFFFFFF.toInt(), // white
            0xFFFF6B35.toInt(), // orange
            0xFF36D1C4.toInt(), // teal
            0xFFFFD23F.toInt(), // yellow
            0xFFFF6FB5.toInt()  // pink
        )
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (isFinished) return
            val remaining = LockService.lockEndTime - System.currentTimeMillis()

            if (remaining <= 0) {
                showCompletion()
                return
            }
            // Lock ended early / was cancelled for some other reason — just leave.
            if (!LockService.isRunning) {
                finish()
                return
            }

            val secLeft = (remaining / 1000).toInt()
            tvTimer.text = formatCountdown(secLeft)

            if (secLeft != lastSecShown) {
                lastSecShown = secLeft
                if (secLeft in 1..3) vibrateTick()   // 3 … 2 … 1 buzz
            }
            handler.postDelayed(this, TICK_MS)
        }
    }

    private val bounceRunnable = object : Runnable {
        override fun run() {
            if (isFinished) return
            stepBounce()
            handler.postDelayed(this, FRAME_MS)
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

        lockRoot          = findViewById(R.id.lockRoot)
        tvTimer           = findViewById(R.id.tvTimer)
        completionOverlay = findViewById(R.id.completionOverlay)
        tvCompletionMsg   = findViewById(R.id.tvCompletionMsg)
        findViewById<Button>(R.id.btnDone).setOnClickListener { finish() }

        // Initial value so it doesn't flash 0:00:00.
        val remaining = (LockService.lockEndTime - System.currentTimeMillis()).coerceAtLeast(0)
        tvTimer.text = formatCountdown((remaining / 1000).toInt())

        handler.post(tickRunnable)
        lockRoot.post { setupBounce() }
        scheduleAutoDim()
    }

    // ─── Bouncing countdown ──────────────────────────────────────────────────

    private fun setupBounce() {
        posX = (lockRoot.width - tvTimer.width) / 2f
        posY = (lockRoot.height - tvTimer.height) / 2f
        tvTimer.translationX = posX
        tvTimer.translationY = posY

        val speed = 2.2f * resources.displayMetrics.density
        velX = speed
        velY = speed
        handler.post(bounceRunnable)
    }

    private fun stepBounce() {
        val maxX = (lockRoot.width - tvTimer.width).toFloat()
        val maxY = (lockRoot.height - tvTimer.height).toFloat()
        if (maxX <= 0f || maxY <= 0f) return

        var bounced = false
        posX += velX
        posY += velY

        if (posX <= 0f)      { posX = 0f;    velX = -velX; bounced = true }
        else if (posX >= maxX) { posX = maxX; velX = -velX; bounced = true }
        if (posY <= 0f)      { posY = 0f;    velY = -velY; bounced = true }
        else if (posY >= maxY) { posY = maxY; velY = -velY; bounced = true }

        if (bounced) {
            colorIndex = (colorIndex + 1) % BOUNCE_COLORS.size
            tvTimer.setTextColor(BOUNCE_COLORS[colorIndex])
        }

        tvTimer.translationX = posX
        tvTimer.translationY = posY
    }

    private fun formatCountdown(totalSec: Int): String {
        val s = totalSec.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return "%d:%02d:%02d".format(h, m, sec)
    }

    private fun vibrateTick() {
        vibrator?.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    // ─── Completion ──────────────────────────────────────────────────────────

    private fun showCompletion() {
        if (isFinished) return
        isFinished = true
        handler.removeCallbacks(tickRunnable)
        handler.removeCallbacks(bounceRunnable)
        handler.removeCallbacks(autoDimRunnable)

        // Make sure the screen is at full brightness for the celebration.
        window.attributes = window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        isDimmed = false

        tvTimer.visibility = View.GONE
        tvCompletionMsg.text = "You stayed off your phone for ${formatDuration(LockService.lockTotalMinutes)}.\nNice work. 💪"
        completionOverlay.visibility = View.VISIBLE

        // Celebratory finish pattern.
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120, 80, 350), -1))
    }

    private fun formatDuration(minutes: Int): String =
        if (minutes < 60) "$minutes min"
        else {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0) "${h}h" else "${h}h ${m}m"
        }

    // ─── Always-on dim mode ──────────────────────────────────────────────────

    private fun scheduleAutoDim() {
        handler.removeCallbacks(autoDimRunnable)
        handler.postDelayed(autoDimRunnable, AUTO_DIM_DELAY_MS)
    }

    private fun enterDim() {
        if (isDimmed || isFinished) return
        isDimmed = true
        window.attributes = window.attributes.apply { screenBrightness = DIM_BRIGHTNESS }
    }

    private fun exitDim() {
        isDimmed = false
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
        handler.removeCallbacks(bounceRunnable)
        handler.removeCallbacks(autoDimRunnable)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() { /* locked — back button does nothing */ }
}
