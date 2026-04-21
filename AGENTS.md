# AI Agent Guidelines for Smartr

This file contains the project conventions and technical rules for all AI coding agents working on the Smartr project.

## Project Structure
- `:mobile`: Android application for handheld devices.
- `:wear`: Wear OS application (Primary focus for active health tracking).

## Commit Conventions
- Always use the **Conventional Commits** specification (e.g., `feat(scope): ...`, `fix(scope): ...`, `chore: ...`).
- **Scopes**: `wear`, `mobile`, `data`, `logic`, `ui`, `worker`, `service`.
- **Subject**: Start with a lowercase letter, use imperative mood (e.g., `feat(wear): add settings screen`).

## Tech Stack & Standards
### Language & Tools
- **Kotlin 1.9/2.0+**: Use `.entries` for Enums. Prefer `val` over `var`.
- **KSP**: Used for Room annotation processing.
- **Gradle**: Kotlin DSL (`.gradle.kts`).

### Wear OS (`:wear`)
- **Compose Material 3**: Standardize on Wear M3 components (`TitleCard`, `AppCard`, `Button`, `Stepper`).
- **Navigation**: Use `SwipeDismissableNavHost` from `androidx.wear.compose.navigation`.
- **Theming**: Use `ColorScheme` and `dynamicColorScheme` (API 35+). Wear OS is dark-themed by design; `lightColorScheme` is rarely used.
- **Health Services**: Use for passive data tracking (`PassiveDataService`).
- **Persistence**: 
    - Use **Room** for structured history (`DailySummary`, `HistoryDatabase`).
    - Use **DataStore Preferences** for simple app settings (`SettingsRepository`).
- **WorkManager**: Use for scheduling registration or background cleanup.

### Handheld (`:mobile`)
- **Compose Material 3**: Use standard M3 components.
- **Data Layer**: Use the Wearable Data Layer API to sync with the watch module.

## Coding Patterns
- **Repository Pattern**: All data access should go through a repository (e.g., `SettingsRepository`).
- **Flow/StateFlow**: UI should observe data as reactive streams.
- **Decoupled Logic**: Keep complex calculations in "Engines" (e.g., `BehaviorInsightsEngine`, `InactivityEngine`).
- **Dependency Injection**: Currently manual or constructor-based; keep it simple unless Hilt/Koin is explicitly introduced.

## Agent Behavior
- **Tool Usage**: Prefer `replace_file_content` or `multi_replace_file_content` for surgical edits.
- **Verification**: After UI changes, use `adb shell input` and `ui_state` to verify navigation and interaction.
- **Context**: Always read relevant `build.gradle.kts` files to verify dependency versions before adding new code.
