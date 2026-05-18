package com.morninglock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        if (alarmId == -1) return

        // Start AlarmService to ring the alarm (sound + vibrate + show screen)
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id", alarmId)
        }
        context.startForegroundService(serviceIntent)
    }
}
