# Helios Alarm Clock

An Android alarm clock app with a built-in HTTP server, designed to be controlled remotely from a Raspberry Pi or any device on the local network.

## Features

- **HTTP API** — Embedded Ktor server (port 8080) for remote alarm management
- **In-app UI** — Set and remove alarms with a Material 3 time picker
- **Reliable alarms** — Uses `AlarmManager.setExactAndAllowWhileIdle()` to fire through Doze mode
- **Full-screen alarm** — Wakes the screen, plays the system alarm sound, and vibrates
- **DND bypass** — Alarm audio uses `USAGE_ALARM` to ring even in Do Not Disturb mode
- **Persistent server** — Foreground service with wake lock keeps the HTTP server alive
- **Boot survival** — Server and alarms reschedule automatically after reboot
- **Auto-cleanup** — Fired alarms are automatically deleted from the database

## HTTP API

All endpoints are served on port `8080`.

### Set an alarm

```
POST /set
Content-Type: application/json

{"hour": 7, "minute": 30, "label": "Wake up"}
```

Returns `201 Created` with `{"id": "<uuid>"}`.

### Remove an alarm

```
POST /rm
Content-Type: application/json

{"id": "<uuid>"}
```

### List alarms

```
GET /list
```

Returns a JSON array of all scheduled alarms.

## Tech Stack

- Kotlin, Jetpack Compose, Material 3
- MVVM architecture with Hilt dependency injection
- Room database for alarm persistence
- Ktor CIO embedded HTTP server
- AlarmManager with exact alarms
- Foreground service (connectedDevice type) for the HTTP server

## Requirements

- Android 8.0+ (API 26)
- Target SDK 36 (Android 16)
- Permissions: exact alarms, notifications, foreground service, wake lock, internet

## Building

```bash
./gradlew assembleRelease
```

The APK will be at `app/build/outputs/apk/release/app-release.apk`.
