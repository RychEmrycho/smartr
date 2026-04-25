# Contributing to Smartr 🤝

First off, thank you for considering contributing to Smartr! It's people like you who make Smartr such a great tool for the community.

## Code of Conduct

By participating in this project, you are expected to uphold our Code of Conduct (be kind, stay professional, and focus on health-positive outcomes).

## How Can I Contribute?

### Reporting Bugs
- Use the GitHub Issue Tracker.
- Describe the bug, hardware (e.g., Oppo Watch X2), and steps to reproduce.

### Suggesting Enhancements
- Open an issue with the tag `enhancement`.
- Explain why the feature would be useful for health/productivity.

### Pull Requests
1. **Branching**: Use `feat/`, `fix/`, or `chore/` prefixes.
2. **Standard**: Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification.
   - Example: `feat(wear): add heart rate-aware inactivity suppression`
3. **Architecture**:
   - Wear logic should live in `:wear`.
   - Data sync logic should use the `Wearable Data Layer API`.
   - UI must follow **Compose Material 3 for Wear OS** standards.
4. **Testing**: If you add logic to the `InactivityEngine`, please include unit tests.

## Development Setup

1. Fork the repository.
2. Clone your fork locally.
3. Open in Android Studio Ladybug+.
4. Select the `wear` run configuration.
5. Ensure `ACTIVITY_RECOGNITION` permission is granted on your test device.

## Commit Message Convention

We follow a strict conventional commit format for our automated changelogs:

- `feat(...)`: A new feature
- `fix(...)`: A bug fix
- `chore(...)`: Changes to the build process or auxiliary tools
- `docs(...)`: Documentation only changes
- `refactor(...)`: A code change that neither fixes a bug nor adds a feature

## 🧪 Manual Testing & Simulation

To test "Smart" features (like sleep or activity detection) without waiting for real-world events, you can simulate user states using ADB commands on your watch.

### 1. Enable Synthetic Mode
Run this first to allow fake data injection:
```bash
adb shell am broadcast -a "whs.USE_SYNTHETIC_PROVIDERS" com.google.android.wearable.healthservices
```

### 2. Simulate User States (Sleep Detection)
Test how the app reacts when you fall asleep:
- **Start Sleeping**: `adb shell am broadcast -a "whs.synthetic.user.START_SLEEPING" com.google.android.wearable.healthservices`
- **Stop Sleeping**: `adb shell am broadcast -a "whs.synthetic.user.STOP_SLEEPING" com.google.android.wearable.healthservices`

### 3. Simulate Activity (Suppress Nudges)
Ensure the app pauses reminders when you are active:
- **Start Walking**: `adb shell am broadcast -a "whs.synthetic.user.START_WALKING" com.google.android.wearable.healthservices`
- **Stop Activity**: `adb shell am broadcast -a "whs.synthetic.user.STOP_EXERCISE" com.google.android.wearable.healthservices`

### 4. Off-Body Simulation (Watch Removal)
Since we use `SensorManager`, you can simulate taking the watch off:
- **Watch Removed (0)**: `adb shell sensor set 34 0`
- **Watch On-Wrist (1)**: `adb shell sensor set 34 1`
*(Note: Sensor ID 34 is the default for Low Latency Off-Body, but it may vary by device).*

---

Happy coding! Let's build a healthier world together.
