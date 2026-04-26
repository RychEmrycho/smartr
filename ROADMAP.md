# Smartr Project Roadmap

## Feature Enhancements
- [ ] **Configurable History Views**: Add a toggle in Settings to switch the Daily Detail screen between "Hourly Summary" (fixed 24h) and "Event Feed" (chronological activity log).
- [ ] **Reminder Analytics**: Track reminder response times and success rates by time-of-day.
- [ ] **Health Connect Integration**: Sync sedentary data with Android Health Connect.

## Technical Debt
- [ ] **Database Optimization**: Review `SedentaryEvent` table growth and implement cleanup for older events (e.g., older than 30 days).
- [ ] **Unit Tests for InactivityEngine**: Expand test coverage for complex multi-event scenarios.
