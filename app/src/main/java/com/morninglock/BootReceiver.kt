package com.morninglock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val db = AppDatabase.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            val alarms = db.alarmDao().getEnabledAlarms()
            alarms.forEach { AlarmScheduler.schedule(context, it) }
        }
    }
}
