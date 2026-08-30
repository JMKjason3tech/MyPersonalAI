# Milestone 6B v7 — Resolver Fix

Fixes the four 6B regression failures reported after v6:
- brightness relative commands
- configuration synonyms for Settings
- navigation synonyms for Settings
- informational follow-up incorrectly inheriting Settings navigation context

The patch also preserves real device-control intent routing and connectivity action routing.

Apply over the current 6B working tree. Run `./gradlew test` before building or committing.
