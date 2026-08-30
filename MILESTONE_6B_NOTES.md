# MyPersonalAI — Milestone 6B

## Scope
Milestone 6B extends the 6A interactive assistant with the first device-control and voice-foundation layer.

### Added
- On-device SpeechRecognizer selection on Android 12+ when an offline recognizer is available.
- `EXTRA_PREFER_OFFLINE` for speech recognition.
- Natural-language `DEVICE_CONTROL` intent for brightness and volume.
- Natural-language `OPEN_APP` intent for installed Android applications.
- `DeviceControlTool` for media volume and system screen brightness.
- `OpenAppTool` using PackageManager and launch intents.
- Conversational Settings response: opening general Settings now reports that it is opening Settings and invites a specific follow-up.
- Existing 5D device/settings routing preserved.
- WRITE_SETTINGS declaration for the user-controlled brightness permission flow.

## Important platform behavior
Offline speech recognition depends on an on-device speech recognizer/language model being available on the phone. The app now prefers the Android on-device recognizer and explicitly requests offline recognition; it does not claim offline recognition is possible when the device has no offline language model.

Changing system-wide screen brightness requires Android's special `WRITE_SETTINGS` access. If that access is not granted, MyPersonalAI opens the Android permission screen instead of pretending it changed brightness. Media volume can be adjusted directly through AudioManager.

## Test targets
- `./gradlew test`
- `./gradlew assembleDebug`
- Online voice input
- Offline voice input with an installed offline speech language model
- `open settings`
- `increase brightness`, `set brightness to 50%`
- `increase volume`, `set volume to 50%`, `mute volume`
- `open WhatsApp` / another installed app
- unknown/non-installed app handling


## v2 fixes
- Fixed contextual follow-ups such as "Open it" from being misclassified as an app launch.
- Expanded volume-control phrase matching, including "turn volume up/down".
- Removed the unused `onNewChat` parameter warning from the welcome hero.
