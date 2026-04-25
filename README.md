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
- **Flexible Time Units**: Unified settings engine allowing seconds, minutes, or hours independently.
- **Manual "Mark as Done"**: Log a break manually from the watch to reset timers instantly.
- **Visual Data**: Sparklines and mini-charts directly on the watch dashboard.
- **Wear OS 5 Complications**: Custom watch face slots for streaks and wellness scores.
- **Dynamic Theming**: Support for dark/light modes and Wear OS 5 dynamic color schemes.

### 📱 Mobile Hub
- **Premium Dashboard**: Material 3 dashboard with wellness gauges and streak tracking.
- **Interactive Trends**: 30-day activity charts using high-fidelity `Canvas` drawing.
- ✅ **Real-time Sync**: Automatic mirroring of health history and settings from watch to phone.
- ✅ **Sedentary Persistence**: Indefinite local history storage using a dedicated mobile Room database.
- ✅ **Health Connect Intelligence**: Automated sleep awareness and cross-platform activity synchronization.

---

## 🗺 Roadmap

Smartr is evolving into a full-scale health ecosystem. Here is our path forward:

### 📱 Phase 1: Mobile Companion
- [x] **Rich Dashboard**: A deep-dive mobile app for long-term health visualization (weekly/monthly/yearly).
- [x] **Persistent Sync**: Real-time Data Layer synchronization between watch and phone.
- [x] **Sedentary Status**: View current sitting time via persistent phone notifications and widgets.

### 🏥 Phase 2: Health Intelligence
- [x] **Health Connect Integration**: Deep integration with the Android health ecosystem.
- [x] **Sleep Awareness**: Automatically suppress reminders when Health Connect detects you are sleeping (reading from apps like Samsung Health, Oura, etc.).
- [x] **Smart Nudge Logic**: Watches now respect your sleep state via real-time mobile synchronization.

### 🚀 Phase 3: The Polish & AI (Next)
- [ ] **Predictive Coaching**: AI-driven reminders that learn your daily schedule and nudge you *before* you reach your limit.
- [ ] **Smart notification**: Custom haptics and sounds for different health events.

---

## 🛠 Tech Stack

- **Core**: Kotlin 2.0+, Coroutines, Flow
- **UI**: Jetpack Compose for Wear OS (Material 3)
- **Persistence**: Room (History), DataStore (Settings)
- **Health**: Wear Health Services (PassiveMonitoringClient)
- **Async**: WorkManager for background registration logic
- **Navigation**: Swipe-to-dismiss Navigation for Wear

---

## 📱 Testing Devices

Tested on the following hardware to ensure performance and sensor accuracy:

*   **Watch**: Oppo Watch X2 (Wear OS 5 / API 35)
*   **Phone**: Samsung Galaxy S23 Ultra (Android 16)

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- Wear OS 4.0+ Device/Emulator

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
