package com.morninglock

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import kotlinx.coroutines.*

class AlarmService : Service() {

    companion object {
        const val ACTION_SNOOZE = "com.morninglock.ACTION_SNOOZE"
        const val ACTION_STOP   = "com.morninglock.ACTION_STOP"
        const val CHANNEL_ID    = "alarm_channel"
        var currentAlarmId: Int = -1
        var isRinging: Boolean = false
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SNOOZE -> {
                snoozeAlarm(intent.getIntExtra("alarm_id", -1))
                return START_NOT_STICKY
            }
            ACTION_STOP -> {
                stopAlarm(intent.getIntExtra("alarm_id", -1), triggeredByUser = true)
                return START_NOT_STICKY
            }
            else -> {
                val alarmId = intent?.getIntExtra("alarm_id", -1) ?: -1
                if (alarmId != -1) startRinging(alarmId)
            }
        }
        return START_STICKY
    }

    private fun startRinging(alarmId: Int) {
        currentAlarmId = alarmId
        isRinging = true

        // Call startForeground immediately (synchronously) to satisfy the Android 8+
        // 5-second rule on every device, before we hit the database.
        startForeground(alarmId + 100, buildLoadingNotification())

        scope.launch {
            val alarm = AppDatabase.getInstance(this@AlarmService).alarmDao().getAlarm(alarmId)
            if (alarm == null) {
                // Alarm was deleted before it fired — clean up instead of leaking the service.
                withContext(Dispatchers.Main) { releaseResources() }
                return@launch
            }

            withContext(Dispatchers.Main) {
                startForeground(alarmId + 100, buildNotification(alarm))
                if (alarm.vibrate) startVibration()
                playRingtone(alarm.ringtoneUri)

                val actIntent = Intent(this@AlarmService, AlarmRingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("alarm_id", alarmId)
                    putExtra("is_primary", alarm.isPrimary)
                    putExtra("alarm_label", alarm.label)
                    putExtra("lock_duration_minutes", alarm.lockDurationMinutes) // KEY FIX: pass exact duration
                }
                startActivity(actIntent)
            }
        }
    }

    fun snoozeAlarm(alarmId: Int) {
        scope.launch {
            val alarm = AppDatabase.getInstance(this@AlarmService).alarmDao().getAlarm(alarmId)
                ?: return@launch
            val totalMin = alarm.hour * 60 + alarm.minute + alarm.snoozeMinutes
            val snoozeAlarm = alarm.copy(hour = (totalMin / 60) % 24, minute = totalMin % 60)
            AlarmScheduler.schedule(this@AlarmService, snoozeAlarm)
            withContext(Dispatchers.Main) { releaseResources() }
        }
    }

    fun stopAlarm(alarmId: Int, triggeredByUser: Boolean) {
        scope.launch {
            val alarm = AppDatabase.getInstance(this@AlarmService).alarmDao().getAlarm(alarmId)

            if (alarm != null) {
                AlarmScheduler.rescheduleForTomorrow(this@AlarmService, alarm)

                // BUG FIX: pass the actual lockDurationMinutes from the alarm object
                if (alarm.isPrimary && triggeredByUser) {
                    withContext(Dispatchers.Main) {
                        val lockIntent = Intent(this@AlarmService, LockService::class.java).apply {
                            putExtra("lock_duration_minutes", alarm.lockDurationMinutes)
                        }
                        startForegroundService(lockIntent)
                    }
                }
            }
            withContext(Dispatchers.Main) { releaseResources() }
        }
    }

    private fun playRingtone(uriString: String) {
        try {
            val uri = if (uriString == "default")
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            else Uri.parse(uriString)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmService", "Ringtone error: ${e.message}")
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 800, 400, 800, 400)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun releaseResources() {
        isRinging = false
        currentAlarmId = -1
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildLoadingNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("⏰ Alarm")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()
    }

    private fun buildNotification(alarm: Alarm): Notification {
        val label = alarm.label.ifEmpty { if (alarm.isPrimary) "Primary Alarm" else "Alarm" }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("⏰ $label")
            .setContentText(alarm.timeLabel())
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Alarm", NotificationManager.IMPORTANCE_HIGH
        ).apply { setSound(null, null) }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        mediaPlayer?.release()
        vibrator?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
