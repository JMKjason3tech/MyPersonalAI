# Milestone 5D — Natural Language Intelligence

## v2 correction
- Fixed nullable target compilation error in `IntentResolver`.
- Fixed notification settings compilation error by using the Android 13+ action string directly, so the project does not depend on a compileSdk constant that may be unavailable.
- Added a safe general Android Settings fallback for older Android versions.

## Validation rule
Do not stage, commit, or push until the corrected overlay builds and the resulting APK is installed and tested on the real device.
