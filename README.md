<div align="center">

# 🔒 MorningLock

**Take back your mornings. Automatically.**

An Android alarm app that locks your phone the moment you wake up — so you start the day intentionally, not on Instagram.

[![Version](https://img.shields.io/badge/version-1.4.0-FF6B35?style=flat-square)](https://github.com/yourusername/MorningLock/releases)
[![Platform](https://img.shields.io/badge/platform-Android%209%2B-brightgreen?style=flat-square)](https://developer.android.com)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)

</div>

---

## What is MorningLock?

Most of us wake up and immediately reach for our phones. Notifications, Instagram, YouTube — before we've even had a glass of water. MorningLock fixes this.

It works as a hybrid of three things:
- **An alarm app** — set one or multiple alarms, choose ringtones and vibration
- **A phone locker** — the moment you stop your *Primary Alarm*, your phone locks itself
- **A Focus timer** — start a lockdown any time from the **Focus** tab, for anywhere from 1 minute to 12 hours

For your chosen duration, only phone calls are available. Everything else is blocked. No Instagram. No YouTube. No doomscrolling while brushing your teeth.

The app has two tabs at the bottom: an **alarm-clock icon** (your alarms) and a **timer icon** (Focus mode).

---

## Download

> 📥 **[Download latest APK → MorningLock-v1.2.apk](https://github.com/yourusername/MorningLock/releases/latest)**

Sideload instructions below.

---

## Features

### v1.4 (Current)
- ✅ **Focus mode** — new bottom tab. Start a phone lockdown any time, not just from an alarm.
- ✅ **Scrollable grain picker** — set the lock/focus duration by scrolling a ruler of "grains" (1 grain = 1 minute), with a haptic buzz on every minute. Pick any exact time from **1 minute to 12 hours** (e.g. 1h 43m). Vertical in Focus, horizontal in the alarm editor.
- ✅ **iPhone-style time wheels** — set the alarm's hour / minute / AM-PM with spinning wheels, with haptics.
- ✅ **Minimalist lock screen** — just one big bouncing countdown (DVD-logo style), white digits with the **seconds in orange**, readable from across the room.
- ✅ **3 · 2 · 1 finale + congrats screen** — the phone buzzes on the last three seconds, then a celebration screen shows how long you stayed off your phone.
- ✅ **Always-on dim** — after ~10s of no touch the lock screen drops to a near-black, low-brightness state to save battery; tap to brighten. (See *Always-On & Battery* below.)
- ✅ **Fresh Gen-Z look** — bolder numbers, gradient buttons, modern orange toggle, new trash icon.
- ✅ **Works on any Android phone** — the lock auto-detects your device's launcher + dialer (not just Nothing/Samsung/Pixel).
- ✅ **Hardened** — alarm receiver no longer publicly exported, backups off, Android 13/14 ready.

### v1.2
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
2. Scroll the **time wheels** to your wake-up time (hour / minute / AM-PM)
3. See **"Alarm in Xh Ym"** — confirms your timing
4. Toggle **Primary Alarm 🔒** ON
5. Scroll the **grain ruler** to set lock duration (buzzes each minute) — any time from 1 min to 12 h
6. Set ringtone and vibration
7. Tap **SAVE ALARM**

Your Primary alarm card shows with an **orange border** and the lock duration info.

### Creating Regular Alarms

Same steps, leave Primary Alarm toggle OFF. These ring normally and don't trigger any lockdown. Useful as backup alarms or reminders.

### Starting a Focus session (no alarm needed)

1. Tap the **timer icon** in the bottom bar
2. Scroll the vertical **grain ruler** to your duration (1 min – 12 h) — you'll feel a buzz per minute
3. Tap **START FOCUS** — your phone locks immediately

### During Lockdown

The lock screen is deliberately minimal — just one big **bouncing countdown** that drifts around the screen (so it's easy to read from a distance and is gentle on OLED screens). The digits are white with the **seconds in orange**.

- At **3 · 2 · 1**, the phone buzzes each second
- When time's up, a **congrats screen** shows how long you stayed off your phone — tap **DONE**
- Need to call someone? Press Home (your launcher stays available) and open your Phone app — calls are always allowed.

### Always-On & Battery (the "dim" feature)

After about **10 seconds without a touch**, the lock screen automatically drops to a **near-black, lowest-brightness state** with a faint, still-moving timer — this is the app saving battery. **Tap the screen** to brighten it back up.

A couple of things worth knowing:
- This is a *software* dim. **True hardware Always-On Display (AOD)** — where the screen is almost completely off — is controlled by your phone's manufacturer and **cannot be turned on by an app.**
- You can always **press the power button** to turn the screen fully off. The lock keeps running in the background, and you can press power again any time to check the countdown.
- So: you do **not** need to press power for it to dim (it dims on its own after ~10s), but pressing power is the most battery-efficient option if you're walking away.

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
| Lock duration wrong | Scroll the grain ruler so the value under the orange center line is what you want before saving |
| WhatsApp call didn't come through | Ensure WhatsApp notifications are enabled in system settings |
| Alarm disappeared after reboot | Open app once; boot receiver reschedules automatically |
| Lock screen stays bright | It dims itself after ~10s of no touch. For a full screen-off, press the power button (the lock keeps running). |

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
