<div align="center">

# 🔒 MorningLock

**Take back your mornings. Automatically.**

An Android alarm app that locks your phone the moment you wake up — so you start the day intentionally, not on Instagram.

[![Version](https://img.shields.io/badge/version-1.2.0-FF6B35?style=flat-square)](https://github.com/yourusername/MorningLock/releases)
[![Platform](https://img.shields.io/badge/platform-Android%209%2B-brightgreen?style=flat-square)](https://developer.android.com)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)

</div>

---

## What is MorningLock?

Most of us wake up and immediately reach for our phones. Notifications, Instagram, YouTube — before we've even had a glass of water. MorningLock fixes this.

It works as a hybrid of two things:
- **An alarm app** — set one or multiple alarms, choose ringtones and vibration
- **A phone locker** — the moment you stop your *Primary Alarm*, your phone locks itself

For your chosen duration (30 min to 2 hours), only phone calls are available. Everything else is blocked. No Instagram. No YouTube. No doomscrolling while brushing your teeth.

---

## Download

> 📥 **[Download latest APK → MorningLock-v1.2.apk](https://github.com/yourusername/MorningLock/releases/latest)**

Sideload instructions below.

---

## Features

### v1.2 (Current)
- ✅ **Smart alarm system** — Primary + regular alarms in one app
- ✅ **Automatic morning lock** — stops Primary alarm → lock begins instantly. No confirmation screens.
- ✅ **Custom lock duration** — slider from 30 min to 2 hours, with haptic tick per minute
- ✅ **"Alarm in Xh Ym" countdown** — shown immediately after setting time (just like stock Clock app)
- ✅ **WhatsApp & WhatsApp Business whitelisted** — incoming calls go through during lock
- ✅ **Rotating motivational quotes** on lock screen — productivity, mindfulness, digital detox
- ✅ **+10 min extend lock** — one tap to extend your focus session from the lock screen
- ✅ **Time-based greeting** — "Good morning, Akash ☀️" / "Coffee time ☕" based on time of day
- ✅ **Inter font** throughout — clean, modern, easy to read
- ✅ **Haptic feedback** — slider ticks, time picker, save button
- ✅ **Minimal sunrise + lock icon** — new custom app icon
- ✅ **Modern delete button** — clean X-in-circle, replaces outdated trash icon
- ✅ **Snooze never triggers lock** — only tapping STOP starts the lockdown

### v1.1
- WhatsApp call support
- Lock duration slider (30–120 min)
- Haptic slider ticks
- "Alarm in X" display

### v1.0
- Core alarm + lock system
- Primary vs regular alarm types
- Orange theme
- Nothing Phone 2 optimised

---

## How It Works

```
You wake up
    ↓
Primary alarm rings (sound + vibration)
    ↓
You tap SNOOZE → alarm rings again in 5 min (no lock)
    ↓
You tap STOP → 🔒 Lockdown begins
    ↓
For your chosen duration (30–120 min):
  ✅ Phone calls — allowed
  ✅ WhatsApp calls — allowed
  ✅ Home screen — visible
  ❌ Instagram, YouTube, Twitter — blocked
  ❌ Chrome, WhatsApp messages — blocked
    ↓
Timer hits zero → phone unlocks automatically
```

---

## Installation

### Requirements
- Android 9.0 (Pie) or higher
- Any Android phone or tablet

### Steps

1. **Download** the APK from the releases page
2. **Transfer** to your phone (WhatsApp to yourself, Google Drive, USB — anything)
3. **Open** the APK file on your phone
4. When prompted: **Settings → Allow from this source → Install**
5. Open **MorningLock**

### One-time permissions (2 minutes)

The app will show warnings for two permissions it needs:

| Permission | Why | Where |
|---|---|---|
| **Usage Access** | To see which app is in foreground and block it | Settings → Apps → Special access → Usage access → MorningLock → ON |
| **Display over other apps** | To show the lock screen on top of everything | Settings → Apps → MorningLock → Display over other apps → ON |

Both are one-time. You'll never be asked again.

---

## Setup Guide

### Creating your Primary Alarm

1. Tap the orange **+** button
2. Tap the time display → set your wake-up time
3. See **"Alarm in Xh Ym"** — confirms your timing
4. Toggle **Primary Alarm 🔒** ON
5. Use the **slider** to set lock duration (feel haptic tick each minute)
6. Set ringtone and vibration
7. Tap **SAVE ALARM**

Your Primary alarm card shows with an **orange border** and the lock duration info.

### Creating Regular Alarms

Same steps, leave Primary Alarm toggle OFF. These ring normally and don't trigger any lockdown. Useful as backup alarms or reminders.

### During Lockdown

The lock screen shows:
- Live countdown timer
- Unlock time ("Unlocks at 9:15 AM")
- A rotating motivational quote (changes each session)
- **+ 10 min** button to extend your focus session
- **Open Dialer** button for calls

---

## Device-Specific Notes

### Nothing Phone 2
Works great. One extra step for battery:
**Settings → Apps → MorningLock → Battery → Unrestricted**

### Samsung (Tab S9 FE and others)
Two extra steps:
1. **Settings → Battery → Background usage limits → MorningLock → Unrestricted**  
2. **Settings → Apps → MorningLock → Battery → Unrestricted**

### OnePlus / Realme / Xiaomi (MIUI/OxygenOS)
Enable autostart:
**Settings → Apps → App management → MorningLock → Autostart → Enable**

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Alarm doesn't ring with screen off | Battery → MorningLock → Unrestricted |
| Lock screen doesn't appear | Grant Overlay permission |
| Lock duration still shows 30 min | Delete and recreate the alarm; pull slider past 30 before saving |
| WhatsApp call didn't come through | Ensure WhatsApp notifications are enabled in system settings |
| Alarm disappeared after reboot | Open app once; boot receiver reschedules automatically |
| +10 min button doesn't update time display | Known minor UI delay — timer updates within 1 second |

---

## Building from Source

If you want to build yourself or contribute:

### Using Replit (no laptop needed)

1. Go to [replit.com](https://replit.com) → Create Repl → **Bash** template
2. Upload the project folder
3. In Shell:
```bash
cd MorningLock && chmod +x build.sh && ./build.sh
```
4. Download APK from `app/build/outputs/apk/debug/app-debug.apk`

### Using Android Studio

1. Clone or download the repo
2. Open in Android Studio (Arctic Fox or newer)
3. `Build → Build APK`
4. Install via ADB or transfer to phone

---

## Roadmap

### v1.3 (Planned)
- [ ] Home screen widget (next alarm + toggle)
- [ ] Monthly stats & streak tracker
- [ ] Gentle fade-in alarm volume (sunrise alarm)
- [ ] Custom lock screen message (personal motivation)
- [ ] Breathing animation on lock screen
- [ ] Snooze counter ("Snoozed 2 times")
- [ ] Lock bypass PIN (emergency exit with delay)
- [ ] Samsung Edge Lighting / Nothing Glyph integration

---

## Contributing

Pull requests welcome. If you find a bug, open an issue with:
- Your device model and Android version
- What you expected to happen
- What actually happened

---

## License

MIT License — do whatever you want with it.

---

<div align="center">

Built to solve one problem: **stop doomscrolling before breakfast.**

Made with ☀️ and Claude

</div>
