package com.morninglock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {

    fun schedule(context: Context, alarm: Alarm) {
        if (!alarm.isEnabled) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntentFor(context, alarm)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    }

    fun cancel(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntentFor(context, alarm))
    }

    fun rescheduleForTomorrow(context: Context, alarm: Alarm) {
        if (!alarm.isEnabled || alarm.repeatDays == 0) return // one-time, don't repeat
        schedule(context, alarm)
    }

    private fun pendingIntentFor(context: Context, alarm: Alarm): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.morninglock.FIRE_ALARM"
            putExtra("alarm_id", alarm.id)
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
