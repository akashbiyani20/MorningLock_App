<div align="center">

# 🔒 MorningLock

**Lock your phone so your mornings — and your focus time — belong to you, not the feed.**

[![Version](https://img.shields.io/badge/version-1.9-FF6B35?style=flat-square)](#)
[![Platform](https://img.shields.io/badge/Android-9%2B-brightgreen?style=flat-square)](#)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)

</div>

---

## Why

Most of us reach for the phone before we're even out of bed, and again every time we should be working. Willpower runs out fast. MorningLock removes the choice: it **locks your phone for a set time** so the only thing you *can* do is put it down. Calls still come through — everything else waits.

## How it works

Two ways to start a lock:

- **🔔 Wake-up lock** — set a *Primary Alarm*. When you stop it in the morning, your phone locks automatically.
- **🎯 Focus mode** — pick any duration and lock your phone right now for deep work.

During the lock, opening any blocked app just bounces you back to a full-screen countdown. When the timer ends, the phone unlocks itself.

## Features

- **Smart alarms** — iPhone-style spinning time wheels, custom ringtone, vibrate.
- **Focus timer** — quick presets (5m · 10m · 30m · 1h · 3h) plus a scrollable grain dial for any exact time, from 1 minute to 12 hours.
- **Automatic lockdown** — stop the Primary Alarm and the lock begins instantly. No confirmation screens.
- **Calls always allowed** — your dialer and incoming calls keep working; everything else is blocked.
- **Flip-clock lock screen** — a big, calm countdown that rotates with your phone and dims itself to save battery.
- **Finish ritual** — a gentle 3·2·1 buzz and a "nice work" screen when your time is up.
- **Works on any Android phone** — detects your launcher and dialer automatically, no brand lock-in.
- **Private by design** — no internet permission, no accounts, no data leaves your device.

## Setup (one time)

1. Install the APK.
2. Grant the two permissions the app asks for:
   - **Usage Access** — to see which app is open and block it.
   - **Display over other apps** — to show the lock screen.

That's it. You'll never be asked again.

> **Note on battery:** the lock screen dims itself after ~10s. To save even more power, just press the power button — the lock keeps running and you can check the countdown any time.

## Build it yourself

**Replit (no PC needed):**
```bash
chmod +x build.sh && ./build.sh
```
**Android Studio:** open the project → Build → Build APK.

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## License

MIT — do whatever you want with it.

<div align="center">

Built to solve one problem: **stop doomscrolling, start living.**

</div>
