package com.morninglock

import android.os.*
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
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
    private val orangeColor by lazy { getColor(R.color.orange_primary) }

    // Bounce state
    private var posX = 0f
    private var posY = 0f
    private var velX = 0f
    private var velY = 0f

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
        const val AUTO_DIM_DELAY_MS = 10_000L
        const val DIM_BRIGHTNESS = 0.01f
        const val DIM_ALPHA = 0.4f
        const val TICK_MS = 250L
        const val FRAME_MS = 16L
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
            renderTimer(secLeft)

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

        val remaining = (LockService.lockEndTime - System.currentTimeMillis()).coerceAtLeast(0)
        renderTimer((remaining / 1000).toInt())

        handler.post(tickRunnable)
        lockRoot.post { setupBounce() }
        scheduleAutoDim()
    }

    // ─── Countdown rendering (white, with seconds in orange) ───────────────────

    private fun renderTimer(totalSec: Int) {
        val s = totalSec.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        val str = "%d:%02d:%02d".format(h, m, sec)
        val span = SpannableString(str)
        val secStart = str.length - 2   // the two seconds digits
        span.setSpan(
            ForegroundColorSpan(orangeColor),
            secStart, str.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvTimer.text = span
    }

    // ─── Bouncing countdown ────────────────────────────────────────────────────

    private fun setupBounce() {
        posX = (lockRoot.width - tvTimer.width) / 2f
        posY = (lockRoot.height - tvTimer.height) / 2f
        tvTimer.translationX = posX
        tvTimer.translationY = posY

        val speed = 1.4f * resources.displayMetrics.density   // a bit slower
        velX = speed
        velY = speed
        handler.post(bounceRunnable)
    }

    private fun stepBounce() {
        val maxX = (lockRoot.width - tvTimer.width).toFloat()
        val maxY = (lockRoot.height - tvTimer.height).toFloat()
        if (maxX <= 0f || maxY <= 0f) return

        posX += velX
        posY += velY

        if (posX <= 0f)        { posX = 0f;    velX = -velX }
        else if (posX >= maxX) { posX = maxX;  velX = -velX }
        if (posY <= 0f)        { posY = 0f;    velY = -velY }
        else if (posY >= maxY) { posY = maxY;  velY = -velY }

        tvTimer.translationX = posX
        tvTimer.translationY = posY
    }

    private fun vibrateTick() {
        vibrator?.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    // ─── Completion ────────────────────────────────────────────────────────────

    private fun showCompletion() {
        if (isFinished) return
        isFinished = true
        handler.removeCallbacks(tickRunnable)
        handler.removeCallbacks(bounceRunnable)
        handler.removeCallbacks(autoDimRunnable)

        window.attributes = window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        isDimmed = false

        tvTimer.visibility = View.GONE
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

    // ─── Always-on dim (near-black, faint timer) ──────────────────────────────

    private fun scheduleAutoDim() {
        handler.removeCallbacks(autoDimRunnable)
        handler.postDelayed(autoDimRunnable, AUTO_DIM_DELAY_MS)
    }

    private fun enterDim() {
        if (isDimmed || isFinished) return
        isDimmed = true
        tvTimer.alpha = DIM_ALPHA
        window.attributes = window.attributes.apply { screenBrightness = DIM_BRIGHTNESS }
    }

    private fun exitDim() {
        isDimmed = false
        tvTimer.alpha = 1f
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
