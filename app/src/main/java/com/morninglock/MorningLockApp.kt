package com.morninglock

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MorningLockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("morninglock_prefs", MODE_PRIVATE)
        AppCompatDelegate.setDefaultNightMode(
            if (prefs.getBoolean("dark_mode", true)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
