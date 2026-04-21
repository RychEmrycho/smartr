# Smartr ⌚️

[![Android](https://img.shields.io/badge/Platform-Wear%20OS%204+-3DDC84?logo=android&logoColor=white)](https://developer.android.com/wear)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)

**Smartr** is a premium, open-source sedentary behavior coaching platform for Wear OS. Unlike passive step trackers, Smartr focuses on *active habit correction* by monitoring prolonged inactivity and providing intelligent nudges to move, stretch, and hydrate.

Designed with a "Wear-First" philosophy, it leverages low-power passive health sensors to ensure minimal battery impact while delivering maximum health value.

---

## ✨ Key Features (Implemented)

### 🩺 Wellness Logic
- **Wellness Score (0-100)**: A proprietary algorithm that calculates your daily movement efficiency based on sitting duration and reminder responsiveness.
- **Dynamic Streaks**: Track consecutive days of meeting your movement goals.
- **Passive Monitoring**: Uses Wear Health Services to track inactivity in the background without draining battery.

### 🎨 Premium Wear UI
- **Material 3 Design**: Fully compliant with the latest Wear OS Design System.
- **Activity Insights**: At-a-glance dashboard showing Average Sitting Time, Reminder Response Rate, and Activity Trends.
- **On-Watch History**: Rapid access to the last 10 days of behavior summaries.

### ⚙️ Deep Customization
- **Intelligent Quiet Hours**: Suppress notifications during sleep or focus time.
- **Granular Thresholds**: Customize sit limits (15-240m) and reminder intervals.
- **Dynamic Theming**: Support for dark/light modes and Wear OS 5 dynamic color schemes.

---

## 🗺 Roadmap

Smartr is evolving into a full-scale health ecosystem. Here is our path forward:

### 📍 Phase 1: UX Power (Current Focus)
- [ ] **Flexible Time Units**: Unified settings engine allowing seconds, minutes, or hours independently.
- [ ] **Manual "Mark as Done"**: Log a break manually from the watch to reset timers instantly.
- [ ] **Visual Data**: Sparklines and mini-charts directly on the watch dashboard.
- [ ] **Wear OS 5 Complications**: Custom watch face slots for streaks and wellness scores (Optimized for **Oppo Watch X2**).

### 📱 Phase 2: The Companion Hub
- [ ] **S23 Ultra Hub**: A rich mobile app for long-term data visualization (weekly/monthly/yearly).
- [ ] **Persistent Sync**: Real-time Data Layer synchronization between watch and phone.
- [ ] **Live Status**: See your current "Sitting Time" from a persistent notification on your phone.
- [ ] **Phone Widgets**: Home screen shortcuts for quick logging and status checks.

### 🏥 Phase 3: Health Intelligence
- [ ] **Health Connect Integration**: Sync data with Google Fit, Oura, or MyFitnessPal.
- [ ] **Sleep Awareness**: Automatically suppress reminders when Health Connect detects you are sleeping.
- [ ] **Predictive Coaching**: AI-driven reminders that learn your daily schedule and nudge you *before* you reach your limit.

---

## 🛠 Tech Stack

- **Core**: Kotlin 2.0+, Coroutines, Flow
- **UI**: Jetpack Compose for Wear OS (Material 3)
- **Persistence**: Room (History), DataStore (Settings)
- **Health**: Wear Health Services (PassiveMonitoringClient)
- **Async**: WorkManager for background registration logic
- **Navigation**: Swipe-to-dismiss Navigation for Wear

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- Wear OS 4.0+ Device/Emulator
- (Recommended) Physical device like **Oppo Watch X2** or **Samsung Galaxy Watch** for sensor testing.

### Build & Run
1. Clone the repository.
2. Open in Android Studio.
3. Select the `wear` run configuration.
4. Deploy to your wearable device.

```bash
# Build Debug APKs
./gradlew :wear:assembleDebug
./gradlew :mobile:assembleDebug
```

---

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details on our code of conduct, development setup, and pull request process.

---

## 📄 License

Distributed under the GNU GPL v3 License. See `LICENSE` for more information.

---
*Built with ❤️ for a healthier lifestyle.*
