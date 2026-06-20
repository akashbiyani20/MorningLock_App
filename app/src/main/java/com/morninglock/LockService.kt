package com.morninglock

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.telecom.TelecomManager
import android.util.Log

class LockService : Service() {

    companion object {
        const val CHANNEL_ID = "lock_channel"
        var isRunning = false
        var lockEndTime: Long = 0L
        var lockTotalMinutes: Int = 0   // duration this session was started with

        // Whitelisted always (system + dialer)
        val WHITELIST_ALWAYS = setOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.nothing.dialer",
            "com.android.phone",
            "com.morninglock",
            "com.android.systemui",
            "com.nothing.launcher",
            "com.android.launcher",
            "com.google.android.apps.nexuslauncher",
            "com.samsung.android.app.launcher",
            "com.sec.android.app.launcher"
        )

        // WhatsApp whitelisted for incoming calls
        // These are allowed fully during lock so incoming calls can be received
        val WHITELIST_CALLS = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b"   // WhatsApp Business
        )
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var usageStatsManager: UsageStatsManager

    // Packages detected at runtime for THIS device (launcher + dialer), so blocking
    // adapts to any OEM rather than relying only on the hardcoded list above.
    private val dynamicWhitelist = mutableSetOf<String>()

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
        detectDeviceApps()
    }

    /** Resolve this device's launcher(s) and dialer so the lock never traps the user. */
    private fun detectDeviceApps() {
        try {
            // All home/launcher apps (current + fallback) — keeps the home button working.
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .forEach { dynamicWhitelist.add(it.activityInfo.packageName) }

            // Default dialer (so "Open Dialer" and incoming calls work on any OEM).
            val telecom = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecom?.defaultDialerPackage?.let { dynamicWhitelist.add(it) }

            // Whatever resolves a phone-dial intent (covers OEM phone apps).
            val dialIntent = Intent(Intent.ACTION_DIAL)
            packageManager.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName?.let { dynamicWhitelist.add(it) }

            Log.d("MorningLock", "Dynamic whitelist: $dynamicWhitelist")
        } catch (e: Exception) {
            Log.e("MorningLock", "detectDeviceApps failed: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // BUG FIX: Read duration from intent, default 30 only as fallback
        val durationMinutes = intent?.getIntExtra("lock_duration_minutes", 30) ?: 30
        Log.d("MorningLock", "Starting lock for $durationMinutes minutes")

        isRunning = true
        lockTotalMinutes = durationMinutes
        lockEndTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)

        startForeground(1, buildNotification())
        launchLockActivity()
        handler.post(monitorRunnable)
        return START_STICKY
    }

    private fun checkAndBlock() {
        val foreground = getForegroundApp() ?: return
        if (!isAllowed(foreground)) {
            launchLockActivity()
        }
    }

    private fun isAllowed(pkg: String): Boolean {
        if (dynamicWhitelist.contains(pkg)) return true // this device's launcher + dialer
        if (WHITELIST_ALWAYS.contains(pkg)) return true
        if (WHITELIST_CALLS.contains(pkg)) return true  // WhatsApp allowed
        if (pkg.contains("dialer", ignoreCase = true)) return true
        if (pkg.contains("launcher", ignoreCase = true)) return true
        if (pkg.contains("systemui", ignoreCase = true)) return true
        if (pkg.contains("incallui", ignoreCase = true)) return true
        return false
    }

    private fun launchLockActivity() {
        val intent = Intent(this, LockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
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

    private fun remainingMinutes() = ((lockEndTime - System.currentTimeMillis()) / 1000 / 60).coerceAtLeast(0)
    private fun remainingSeconds() = ((lockEndTime - System.currentTimeMillis()) / 1000 % 60).coerceAtLeast(0)

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
