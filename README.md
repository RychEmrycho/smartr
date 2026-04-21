# Smartr

Smartr is a Wear OS app focused on sedentary behavior coaching:

- tracks prolonged inactivity from passive health/sensor updates
- reminds users to pause, hydrate, and stretch
- suppresses reminders during configured quiet hours
- records reminder and behavior history for basic insights

This repo currently contains a Wear-first MVP plus a lightweight mobile companion stub.

## Why the name "Smartr"?

`Smartr` is intentionally named as:

- a compact form that sounds like **"smarter"**
- a nod to the app's core purpose: **smart reminder**

So the name reflects both the product personality and objective: helping users build smarter daily habits through timely reminders.

## Modules

- `wear` - main Wear OS app (MVP implementation lives here)
- `mobile` - companion Android app placeholder (phase 2)

## Implemented MVP features

- Passive monitoring registration worker:
  - `wear/.../worker/PassiveRegistrationWorker.kt`
- Passive listener service:
  - `wear/.../service/PassiveDataService.kt`
- Inactivity engine with configurable logic:
  - sit threshold
  - repeat reminder interval
  - quiet hours
- On-watch settings persistence via DataStore:
  - `wear/.../data/SettingsRepository.kt`
- Reminder notifications with acknowledge action:
  - `wear/.../reminder/ReminderScheduler.kt`
  - `wear/.../reminder/ReminderActionReceiver.kt`
- History tracking via Room:
  - reminders sent
  - reminders acknowledged
  - interval-based sedentary minute accumulation
- Basic insights shown in Wear UI:
  - average sedentary minutes
  - total reminders
  - reminder response rate
  - last passive callback age

## Tech stack

- Kotlin
- Jetpack Compose for Wear OS
- Wear Health Services (`PassiveMonitoringClient`)
- WorkManager
- DataStore Preferences
- Room

## Requirements

- Android Studio (latest stable recommended)
- JDK 17
- Wear OS device/emulator (Wear OS 3+)
- For physical watch testing: permission grants on device (`ACTIVITY_RECOGNITION`, notifications)

## Run in Android Studio

1. Open this folder in Android Studio.
2. Sync Gradle.
3. Select `wear` run configuration.
4. Deploy to Wear emulator or watch.

## Build APK

### Option A: Android Studio (easiest)

- Build > Build Bundle(s) / APK(s) > Build APK(s)
- Output (debug APK):
  - `wear/build/outputs/apk/debug/wear-debug.apk`
  - `mobile/build/outputs/apk/debug/mobile-debug.apk`

### Option B: Command line

The project includes a Gradle wrapper, so you can build using:

```bash
./gradlew :wear:assembleDebug
./gradlew :mobile:assembleDebug
```

Release builds:

```bash
./gradlew :wear:assembleRelease
./gradlew :mobile:assembleRelease
```

## Current limitations / next priorities

- Improve movement detection robustness beyond `STEPS_DAILY` deltas.
- Add richer settings UX (sliders/pickers).
- Add charts/timeline history UI.
- Add phone sync and optional Health Connect sleep-aware suppression.
- Add tests for inactivity state machine and reminder cadence.
