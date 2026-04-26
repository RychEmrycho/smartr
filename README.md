# Smartr

Smartr is a simple tool for Wear OS that helps you balance sitting and moving. It monitors your activity in the background and gives you a nudge when it's time to stand up, stretch, or take a walk.

It is built to handle sedentary habits specifically. The app runs entirely on your watch, using the system's background sensors to detect movement, sleep, and whether you are currently wearing the device.

---

## Features

### Tracking
- **Background monitoring**: Uses standard system sensors to track activity with minimal power impact.
- **Wrist detection**: Pauses reminders when the watch is taken off.
- **Sleep detection**: Suppresses notifications while the system detects you are asleep.

### On the watch
- **Vitality System**: A gamified experience with levels, XP, and ranks (Novice to Zen Master).
- **Dashboard**: A quick look at your sitting time, level progress via the Vitality Ring, and 7-day trends.
- **Help & Guide**: Built-in guide explaining the scoring and leveling mechanics.
- **Watch faces**: Streaks and scores available as complications.
- **Manual logs**: Option to record a break manually to reset your timers.

### On the phone
- **Data sync**: Automatically sends your history and settings to your phone.
- **History**: Store and view your progress over weeks and months in a dashboard.

---

## Progress

### Phase 1: Core Watch App (Done)
- [x] Background activity and sedentary tracking.
- [x] Native sleep and off-body detection.
- [x] On-watch dashboard and wellness scores.

### Phase 2: Phone Integration (Done)
- [x] Background synchronization between watch and phone.
- [x] History dashboard for long-term trends.
- [x] Persistent storage for yearly progress.

### Phase 3: Refinement (Done)
- [x] Gamified Vitality system (Levels, XP, Ranks).
- [x] Revamped UI/UX with simplified navigation.
- [x] Integrated Help & Guide for users.
- [ ] Predictive reminders based on your schedule.
- [ ] Custom haptics and sounds.
- [ ] Improved charts and visualizations.

---

## Tech Stack
- **Kotlin 2.0**
- **Jetpack Compose** (Material 3)
- **Room & DataStore** for storage.
- **Wear Health Services** for activity tracking.

---

## Getting Started

### Prerequisites
- Android Studio Ladybug or newer.
- A watch or emulator running Wear OS 4.0+.

### Build & Run
1. Clone the repository.
2. Open in Android Studio.
3. Run the `wear` configuration on your watch.

```bash
# Build APKs
./gradlew :wear:assembleDebug
./gradlew :mobile:assembleDebug
```

---

## Contributing
We welcome contributions. If you're a developer, check out our [Contributing Guide](CONTRIBUTING.md) for technical details and simulation commands.

---

## License
Distributed under the GNU GPL v3 License.
