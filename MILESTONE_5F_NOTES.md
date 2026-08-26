# Milestone 5F — Voice Interaction Bundle

## Baseline

Built from the tested 5E `main` checkpoint.

## Included

1. **Voice input / speech-to-text**
   - Android `SpeechRecognizer` adapter/controller.
   - Explicit microphone runtime permission.
   - Recognized speech is returned as plain text to the existing conversation pipeline.
   - No parallel voice command parser was introduced.

2. **Visible voice states**
   - READY
   - LISTENING
   - PROCESSING
   - ERROR
   - The UI exposes listening, stop, processing, retry, and error feedback.

3. **Text + voice unified conversation**
   - Spoken text is submitted through the existing `ConversationViewModel.sendMessage()` path.
   - Existing 5E contextual intent behavior therefore remains the same for typed and spoken commands.

4. **Cancellation and error handling**
   - Stop/cancel while listening.
   - No speech / no match handling.
   - Permission-denied handling.
   - Speech recognizer unavailable/busy handling.
   - Duplicate recognition is avoided by returning to a non-listening state after a result/error.

## Known deferred issue

The Milestone 5D battery/device-information intent matching issue remains intentionally untouched.

## Validation required in Codespaces

```bash
./gradlew test
./gradlew assembleDebug
```

Then install the APK and test voice interaction on the real Android device.

## Suggested device tests

1. Type `open display settings`.
2. Tap **Mic** and say `open it` — Display Settings should open again using 5E context.
3. Tap **Mic** and say `open wifi settings`.
4. Tap **Mic** and say `open it` — Wi-Fi Settings should open.
5. Type `open it` after the voice command — text should use the updated context.
6. Tap **Mic**, then **Stop** before speaking — no command should be submitted.
7. Test microphone permission denial/retry behavior.
8. Test no-speech / recognition failure behavior.
9. Verify normal text commands and existing 5D Settings routing still work.

## Branch

`milestone-5f-voice-interaction`

## Important

This branch is a development handoff. Do not merge it into `main` until the automated tests, debug build, and real-device tests pass.
