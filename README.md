# 🔥 Burner Alarm

A configurable RuneLite plugin that provides a two-stage alert system when incense burners in your **Player-Owned House (POH)** are about to expire.

---

## ✅ Features

- ⏱️ **Smart timing** based on your **Firemaking level**
- ⚠️ **Two-stage alert system**:
    - Pre-warning notification
    - Follow-up audible alarm
- 🔊 **Custom sound** with adjustable volume
- 🔁 **One alert per burner cycle** — re-lighting cancels pending alerts
- 🔇 **Cooldown system** to prevent spam

---

## ⚙️ How It Works

1. When a burner is lit, a timer starts using the formula:  
   **`200 + Firemaking level` ticks**
2. At a configurable point (default **10 seconds** before burnout is possible), a **notification** is sent.
3. If the burner is not re-lit by the time the random burnout phase starts, a **sound alarm** is played.

> ⚠️ RuneLite tick = 0.6 seconds

---

## 🛠️ Configuration

| Setting                 | Description                                                                 |
|-------------------------|-----------------------------------------------------------------------------|
| **Send Notification**   | Enables a pre-warning notification before burners enter the random phase    |
| **Play Custom Sound**   | Enables the main audible alarm at the moment burnout becomes possible       |
| **Custom Sound Volume** | Adjusts alarm volume in decibels (range: -40 dB to +6 dB)                   |
| **Pre-Warning Lead Time** | Sets how many ticks before burnout the pre-warning should trigger         |