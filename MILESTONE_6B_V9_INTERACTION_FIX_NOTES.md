# MyPersonalAI — Milestone 6B v9 Interaction Fix

This overlay fixes the core 6B interaction-session problem exposed by device testing.

## Included
- User-started foreground microphone session for Android 10.
- Voice session automatically listens again after each recognized utterance.
- Session can continue while Android Settings or another app is opened, subject to device/OS behavior.
- Offline speech requests use Android's normal SpeechRecognizer with `EXTRA_PREFER_OFFLINE` on Android 10, and the on-device recognizer when available on Android 12+.
- Explicit microphone foreground-service declaration.
- Voice errors are surfaced without silently ending the session.
- Existing 6B v8 resolver/device-control files are preserved.

## Important limitation
An Android 10 device still needs a speech-recognition engine with an installed offline language model for true offline recognition. The application cannot manufacture Google's/local speech model if the device does not provide one. The v9 implementation no longer rejects Android 10 merely because `createOnDeviceSpeechRecognizer()` is unavailable.

## Test priority
1. Online voice starts and returns results.
2. Voice remains listening after an action result.
3. Say `open settings`, then say `wifi` without tapping the microphone again.
4. Say `open Chrome`, then issue a follow-up command while the session remains active.
5. Turn network off and repeat a simple voice command.
6. Stop listening explicitly.
7. Verify volume/brightness remain real device controls.

The existing project architecture still respects Android permissions and documented APIs; unsupported privileged operations must be reported rather than faked.
