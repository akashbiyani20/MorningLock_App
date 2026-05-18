# MorningLock 🔒

An alarm app that automatically locks your phone when you wake up.  
Primary alarm rings → you tap Stop → 45-minute (or custom) lockdown begins. Only calls work.

---

## How to Build on Replit (Step by Step)

### Step 1 — Create a Replit project
1. Go to [replit.com](https://replit.com) and sign in
2. Click **+ Create Repl**
3. Choose template: **Bash** (not Android — just Bash)
4. Name it `MorningLock`

### Step 2 — Upload the project
1. In the Replit file browser (left sidebar), click the three dots → **Upload folder**
2. Upload the entire `MorningLock` folder you downloaded

### Step 3 — Run the build
In the Replit Shell (bottom panel), type:
```bash
cd MorningLock
chmod +x build.sh
./build.sh
```
This takes 5–10 minutes the first time (downloads Android SDK). Go make chai ☕

### Step 4 — Download the APK
Once you see `BUILD COMPLETE`, the APK is at:
```
app/build/outputs/apk/debug/app-debug.apk
```
Right-click it in the file browser → **Download**

### Step 5 — Install on your Nothing Phone 2
1. Transfer the APK to your phone (WhatsApp yourself, Google Drive, USB — anything)
2. Open it on your phone
3. It will ask "Allow from this source" → tap Allow
4. Install

---

## First-Time Setup on Phone (2 mins)

Open MorningLock. It will show permission warnings at the top. Tap them:

**1. Usage Access** — lets the app see which app is in foreground so it can block it
- Goes to Settings → find MorningLock → toggle ON → come back

**2. Overlay Permission** — lets the app show the lock screen over other apps
- Toggle ON for MorningLock → come back

Done. Now add your alarms.

---

## How to Use

**Adding a Primary Alarm:**
1. Tap the orange **+** button
2. Set your wake time (e.g. 8:30 AM)
3. Toggle **Primary Alarm** ON
4. Set lock duration with the slider (30 min to 2 hours)
5. Choose ringtone, vibrate — just like a normal alarm
6. Tap **SAVE ALARM**

The Primary alarm card shows with an orange border so you always know which one it is.

**Adding Regular Alarms:**
Same steps, just leave Primary Alarm toggle OFF. These ring normally and don't trigger lockdown.

**Morning flow:**
- 8:30 AM → Primary alarm rings (sound + vibration)
- You tap **Snooze** → rings again in 5 min, still no lock
- You tap **STOP & LOCK** → lock starts immediately
- For 30/45/60 min (whatever you set), only the dialer works
- Phone unlocks automatically when timer hits zero

---

## Nothing Phone 2 — Important Setting

Nothing OS has aggressive battery management. After installing:

**Settings → Apps → MorningLock → Battery → Unrestricted**

Without this, the alarm might not fire when the phone screen is off.

Also: **Settings → Apps → Special app access → Alarms & reminders → MorningLock → Allow**

---

## Customisation

**Change snooze time:** Edit `snoozeMinutes = 5` in `EditAlarmActivity.kt` before building.  
**Add whitelisted apps during lock:** Add package names to `WHITELIST` in `LockService.kt`.  
**Change default lock duration:** Edit `lockDurationMinutes: Int = 30` in `Alarm.kt`.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Alarm doesn't ring | Settings → Battery → MorningLock → Unrestricted |
| Lock screen doesn't appear | Grant Overlay permission |
| App blocked too slow (1-2 sec) | Normal behaviour — checks every second |
| Alarm stopped after reboot | Re-open app once; it reschedules on boot automatically |
| Build fails on Replit | Make sure you uploaded the whole MorningLock folder |
