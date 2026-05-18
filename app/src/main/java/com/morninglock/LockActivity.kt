package com.morninglock

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.TimeUnit

class LockActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var tvUnlockTime: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var lockEndTime: Long = 0L

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (!LockService.isRunning) {
                finish()
                return
            }
            updateCountdown()
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
        val btnCall  = findViewById<Button>(R.id.btnCall)

        lockEndTime = intent.getLongExtra("lock_end_time", LockService.lockEndTime)

        // Show what time it unlocks
        val unlockCal = java.util.Calendar.getInstance().apply { timeInMillis = lockEndTime }
        val h = unlockCal.get(java.util.Calendar.HOUR_OF_DAY)
        val m = unlockCal.get(java.util.Calendar.MINUTE)
        val amPm = if (h < 12) "AM" else "PM"
        val displayH = if (h == 0) 12 else if (h > 12) h - 12 else h
        tvUnlockTime.text = "Unlocks at %d:%02d %s".format(displayH, m, amPm)

        btnCall.setOnClickListener {
            val dialIntent = Intent(Intent.ACTION_DIAL)
            startActivity(dialIntent)
        }

        handler.post(timerRunnable)
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        lockEndTime = intent?.getLongExtra("lock_end_time", LockService.lockEndTime) ?: LockService.lockEndTime
    }

    private fun updateCountdown() {
        val remaining = (lockEndTime - System.currentTimeMillis()).coerceAtLeast(0)
        val mins = TimeUnit.MILLISECONDS.toMinutes(remaining)
        val secs = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
        tvTimer.text = "%d:%02d".format(mins, secs)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }

    override fun onBackPressed() { /* blocked */ }
}
