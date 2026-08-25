# MyPersonalAI — Milestone 5D Fixed v3

This is an overlay ZIP for the MyPersonalAI repository.

## Fixes in v3

- Fixed the resolver contradiction where the 5D tests expected bare `battery`, `storage`, and `wifi` to resolve to `DEVICE_INFO`, but the implementation returned `UNKNOWN`.
- Bare device-information targets now resolve to `DEVICE_INFO` only when the entire input is the target (for example `battery`).
- Unrelated sentences containing those words remain `UNKNOWN` (for example `hello battery` and `random network sentence`).
- Explicit Settings language still wins: `open battery settings`, `configure wifi`, `manage notification settings`, etc. resolve to `OPEN_SETTINGS`.
- General `open settings` resolves specifically to the main Android Settings destination.
- Notification Settings routing now prefers `ACTION_ALL_APPS_NOTIFICATION_SETTINGS` on Android 13/API 33+ and falls back to `ACTION_NOTIFICATION_SETTINGS` on older supported versions.
- Added/updated resolver unit tests covering the above behavior.

## Apply

Extract this ZIP at the repository root and replace existing files when prompted.

After extraction, run:

    ./gradlew testDebugUnitTest

Then build:

    ./gradlew assembleDebug

## Phone test matrix

1. `open settings` -> Android Settings home.
2. `open notification settings` -> notification settings, not Settings home.
3. `notification settings` -> notification settings.
4. `battery` -> device information.
5. `check my battery` -> device information.
6. `hello battery` -> clarification/UNKNOWN; must not open a settings page.
7. `something about storage for me` -> UNKNOWN; must not open a settings page.
8. `open battery settings` -> battery Settings.
9. `configure my wifi` -> Wi-Fi Settings.
10. `device info` -> device information.
