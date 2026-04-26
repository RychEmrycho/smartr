# AI Agent Guidelines for Smartr

This file contains the project conventions and technical rules for all AI coding agents working on the Smartr project.

## Project Structure
- `:mobile`: Handheld application (Mobile companion/dashboard).
- `:wear`: Wear OS application (Primary monitoring and logic hub).

## Commit Conventions
- **Standard**: Always use [Conventional Commits](https://www.conventionalcommits.org/).
- **Types**:
  - `feat`: New feature or user-facing improvement.
  - `fix`: Bug fix.
  - `docs`: Documentation changes (README, AGENTS, etc.).
  - `refactor`: Code change that neither fixes a bug nor adds a feature.
  - `test`: Adding missing tests or correcting existing tests.
  - `chore`: Maintenance tasks (Dependency updates, Gradle tweaks).
- **Scopes**: `wear`, `mobile`, `data`, `logic`, `ui`, `worker`, `service`.
- **Subject**: Start with a lowercase letter, use imperative mood (e.g., `feat(wear): add settings screen`).

## Tech Stack & Standards
### Language & Tools
- **Kotlin 2.0+**: Use `.entries` for Enums. Prefer `val` over `var`.
- **KSP**: Used for Room annotation processing.
- **Gradle**: Kotlin DSL (`.gradle.kts`).

### Wear OS (`:wear`) - API 35+ Target
- **Compose Material 3**: Use Wear M3 components (`TitleCard`, `AppCard`, `Button`, `Stepper`).
- **Layouts**: Use `ScreenScaffold` and `ScalingLazyColumn`. Optimize for different screen shapes/sizes (Circular vs. Square).
- **Navigation**: `SwipeDismissableNavHost` is required.
- **Wear OS 5**: Support advanced complications and haptic feedback profiles where applicable.
- **Wellness Score**: Now part of the broader **Vitality System**.
- **Vitality System**: Uses `BehaviorInsightsEngine` to calculate Levels, XP (+50 per break, -1 per 10m sitting), and Ranks (`Novice`, `Active`, `Flow Master`, `Zen Master`).
- **Persistence**: 
    - **Room**: For structured daily/history data.
    - **DataStore**: For lightweight user preferences.
    - **TrackingStateRepository**: MUST be used to persist critical runtime states (sedentary start time, step counts, off-body status) to survive process death and watch restarts.
- **Standalone Architecture**: The watch is the source of truth. Do NOT rely on phone synchronization for real-time tracking decisions (e.g., sleep or activity).
- **Off-Body Detection**: Use the native `OffBodyService` (SensorManager) to pause tracking. Reminders MUST be suppressed if `PassiveRuntimeStore.isOffBody` is true.

### Handheld (`:mobile`)
- **Rich Dashboard**: The mobile app should provide longer history trends and deeper configuration than the watch.
- **Data Layer**: Use specific path prefixes for synchronization (e.g., `/settings/*`, `/history/*`).

## Coding Patterns
- **Repository Pattern**: All data access MUST go through a repository.
- **MVVM Architecture**: 
    - Use `ViewModel` for all screen-level state and logic.
    - Composables should be stateless or use ViewModels for data.
    - Use `StateFlow` to expose reactive state to the UI (e.g., handling loading/syncing states).
- **UI Performance**: 
    - Avoid object allocations (e.g., `Path()`, `Offset()`) inside `DrawScope` or frequent re-compositions.
    - Use `remember` with keys correctly. 
    - For path drawing, use imperative `path.reset()` and `path.lineTo()` to reuse path objects.
- **Decoupled Logic**: Complex calculations (e.g., Wellness Score) MUST be encapsulated in "Engines" (e.g., `BehaviorInsightsEngine`).
- **No Hardcoding**: All strings must use `strings.xml`. All colors must use `Color.kt` or `colors.xml`. Avoid hardcoded dimension offsets; use standard Material 3 tokens.
- **Dependency Injection**: Use manual constructor-based DI to keep the project lightweight unless a framework is explicitly requested.

## Testing Strategy
- **Unit Tests**: All domain logic in "Engines" MUST have unit tests.
- **Library**: Prefer **MockK** for mocking and standard JUnit 5.
- **Integration**: Use `adb shell input` and `ui_state` for manual/agent-based validation of UI flows.

## Agent Behavior
- **Context**: Always read the relevant `build.gradle.kts` to verify dependency versions before adding new libraries.
- **Surgical Edits**: Prefer `replace_file_content` or `multi_replace_file_content` over complete file overwrites.
- **Commit Approval**: Agents MUST NOT commit changes without explicit user approval. Always summarize the changes and ask "Shall I commit these changes?" before running any git commit commands.
- **Verification**: After modifying UI or Logic, verify the build passes via `./gradlew assembleDebug`.
