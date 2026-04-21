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

---

Happy coding! Let's build a healthier world together.
