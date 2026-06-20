package com.morninglock

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AlarmRingActivity : AppCompatActivity() {

    private var alarmId: Int = -1
    private var isPrimary: Boolean = false
    private var lockDuration: Int = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen, turn on screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(R.layout.activity_alarm_ring)

        alarmId     = intent.getIntExtra("alarm_id", -1)
        isPrimary   = intent.getBooleanExtra("is_primary", false)
        lockDuration = intent.getIntExtra("lock_duration_minutes", 30)
        val label   = intent.getStringExtra("alarm_label") ?: ""

        val tvLabel     = findViewById<TextView>(R.id.tvAlarmLabel)
        val tvType      = findViewById<TextView>(R.id.tvAlarmType)
        val tvLockNote  = findViewById<TextView>(R.id.tvLockNote)
        val btnStop     = findViewById<Button>(R.id.btnStop)
        val btnSnooze   = findViewById<Button>(R.id.btnSnooze)

        tvLabel.text = label.ifEmpty { if (isPrimary) "Wake Up" else "Alarm" }

        if (isPrimary) {
            tvType.text = "🔒 Primary Alarm"
            tvType.setTextColor(getColor(R.color.orange_primary))
            tvLockNote.text = "Stopping this alarm starts a ${lockDuration}-minute lock.\nOnly calls will be available."
            tvLockNote.visibility = android.view.View.VISIBLE
            btnStop.setBackgroundColor(getColor(R.color.orange_primary))
            btnStop.text = "STOP & LOCK"
        } else {
            tvType.text = "Alarm"
            tvLockNote.visibility = android.view.View.GONE
            btnStop.text = "STOP"
        }

        btnStop.setOnClickListener {
            sendActionToService(AlarmService.ACTION_STOP)
            finish()
        }

        btnSnooze.setOnClickListener {
            sendActionToService(AlarmService.ACTION_SNOOZE)
            finish()
        }
    }

    private fun sendActionToService(action: String) {
        val intent = Intent(this, AlarmService::class.java).apply {
            this.action = action
            putExtra("alarm_id", alarmId)
        }
        startService(intent)
    }

    // Prevent back button escape
    override fun onBackPressed() { /* locked */ }
}
