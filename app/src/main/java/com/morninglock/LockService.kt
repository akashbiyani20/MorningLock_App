package com.morninglock

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log

class LockService : Service() {

    companion object {
        const val CHANNEL_ID = "lock_channel"
        var isRunning = false
        var lockEndTime: Long = 0L

        val WHITELIST = setOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.phone",
            "com.morninglock",
            "com.android.systemui",
            "com.nothing.launcher",       // Nothing Phone launcher
            "com.android.launcher",
            "com.google.android.apps.nexuslauncher"
        )
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var usageStatsManager: UsageStatsManager

    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (System.currentTimeMillis() >= lockEndTime) {
                Log.d("MorningLock", "Lock session complete.")
                stopSelf()
                return
            }
            checkAndBlock()
            updateNotification()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val durationMinutes = intent?.getIntExtra("lock_duration_minutes", 30) ?: 30
        isRunning = true
        lockEndTime = System.currentTimeMillis() + durationMinutes * 60 * 1000L

        startForeground(1, buildNotification())

        // Show lock screen immediately
        launchLockActivity()

        handler.post(monitorRunnable)
        return START_STICKY
    }

    private fun checkAndBlock() {
        val foreground = getForegroundApp() ?: return
        if (!isWhitelisted(foreground)) {
            launchLockActivity()
        }
    }

    private fun launchLockActivity() {
        val remaining = ((lockEndTime - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
        val intent = Intent(this, LockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("seconds_remaining", remaining)
            putExtra("lock_end_time", lockEndTime)
        }
        startActivity(intent)
    }

    private fun getForegroundApp(): String? {
        val now = System.currentTimeMillis()
        return usageStatsManager
            .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 5000, now)
            ?.filter { it.lastTimeUsed > 0 }
            ?.maxByOrNull { it.lastTimeUsed }
            ?.packageName
    }

    private fun isWhitelisted(pkg: String): Boolean {
        if (WHITELIST.contains(pkg)) return true
        if (pkg.contains("dialer", ignoreCase = true)) return true
        if (pkg.contains("launcher", ignoreCase = true)) return true
        if (pkg.contains("systemui", ignoreCase = true)) return true
        return false
    }

    private fun remainingMinutes() =
        ((lockEndTime - System.currentTimeMillis()) / 1000 / 60).coerceAtLeast(0)

    private fun remainingSeconds() =
        ((lockEndTime - System.currentTimeMillis()) / 1000 % 60).coerceAtLeast(0)

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("🔒 MorningLock Active")
            .setContentText("${remainingMinutes()}m ${remainingSeconds()}s remaining · Only calls allowed")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1, buildNotification())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Lock Session", NotificationManager.IMPORTANCE_LOW
        ).apply { setShowBadge(false) }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(monitorRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
