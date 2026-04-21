# Smartr

Wear OS sedentary behavior reminder app scaffold.

## Current MVP foundation

- Multi-module project (`wear` + `mobile`).
- Wear settings persistence via DataStore:
  - sitting threshold
  - reminder repeat interval
  - quiet hours
- Inactivity decision engine with quiet-hour gating and repeat logic.
- Notification reminder scheduler (stand/drink/stretch prompts).
- History persistence base using Room and simple insights calculator.
- Boot receiver + worker hook for restoring passive monitoring registration.

## Next implementation step

Implement `PassiveMonitoringClient` registration and callback handling in:

- `wear/src/main/java/com/smartr/wear/worker/PassiveRegistrationWorker.kt`
- `wear/src/main/java/com/smartr/wear/service/PassiveDataService.kt`

Then connect those callbacks to `InactivityEngine` + `HistoryRepository`.
