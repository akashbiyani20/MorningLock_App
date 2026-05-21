package com.morninglock

import android.content.Intent
import android.os.*
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.util.concurrent.TimeUnit

class LockActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var tvUnlockTime: TextView
    private lateinit var tvQuote: TextView
    private lateinit var btnExtend: Button
    private val handler = Handler(Looper.getMainLooper())

    // Extend broadcasts to LockService via companion
    companion object {
        const val ACTION_EXTEND = "com.morninglock.ACTION_EXTEND"
        const val EXTEND_MINUTES = 10

        val QUOTES = listOf(
            "The morning is the rudder of the day.",
            "Win the morning, win the day.",
            "Discipline is choosing between what you want now and what you want most.",
            "Your future self is watching you right now through memories.",
            "One hour of focused work beats three hours of distracted scrolling.",
            "A calm morning is a productive day.",
            "The phone can wait. Your mind cannot.",
            "Stillness is where clarity is born.",
            "Great things never come from comfort zones.",
            "Be present. The feed will still be there later.",
            "Every morning you have a choice. Choose intentionally.",
            "The most successful people protect their mornings.",
            "Your attention is your most valuable asset. Guard it.",
            "Digital detox begins with a single locked screen.",
            "Mindfulness isn't about the phone — it's about the moment.",
            "You don't need to check. Nothing is as urgent as it feels.",
            "Start strong. The morning shapes the rest.",
            "Breathe. Ground yourself. The world can wait 45 minutes.",
            "Focus is a superpower in a distracted world.",
            "Small morning habits create massive life changes."
        )
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (!LockService.isRunning) {
                finish()
                return
            }
            updateCountdown()
            updateUnlockTime()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(R.layout.activity_lock)

        tvTimer      = findViewById(R.id.tvTimer)
        tvUnlockTime = findViewById(R.id.tvUnlockTime)
        tvQuote      = findViewById(R.id.tvQuote)
        val btnCall  = findViewById<Button>(R.id.btnCall)
        btnExtend    = findViewById(R.id.btnExtend)

        // Pick a random quote from the list
        tvQuote.text = QUOTES.random()

        updateUnlockTime()

        btnCall.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL))
        }

        btnExtend.setOnClickListener {
            // Add 10 minutes to lock end time
            LockService.lockEndTime += EXTEND_MINUTES * 60 * 1000L
            updateUnlockTime()
            // Brief haptic feedback
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            vib.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            btnExtend.text = "+10 min added ✓"
            handler.postDelayed({ btnExtend.text = "+ 10 min" }, 1500)
        }

        handler.post(timerRunnable)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        updateUnlockTime()
    }

    private fun updateCountdown() {
        val remaining = (LockService.lockEndTime - System.currentTimeMillis()).coerceAtLeast(0)
        val mins = TimeUnit.MILLISECONDS.toMinutes(remaining)
        val secs = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
        tvTimer.text = "%d:%02d".format(mins, secs)
    }

    private fun updateUnlockTime() {
        val cal = Calendar.getInstance().apply { timeInMillis = LockService.lockEndTime }
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        val amPm = if (h < 12) "AM" else "PM"
        val displayH = if (h == 0) 12 else if (h > 12) h - 12 else h
        tvUnlockTime.text = "Unlocks at %d:%02d %s".format(displayH, m, amPm)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() { /* locked — back button does nothing */ }
}
