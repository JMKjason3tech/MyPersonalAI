# Milestone 5F — Voice Interaction & Assistant UI Rebuild

## Baseline

Built from the current tested `main` checkpoint and the existing 5F development branch. The 5F rebuild keeps the established conversation/agent/tool architecture and adds the assistant-facing voice interaction layer.

## Included

1. **Full assistant conversation UI**
   - Assistant identity/header and ready/thinking/speaking status.
   - Conversation bubbles for user and AI messages.
   - Empty-state guidance and example command.
   - Text composer and send action.

2. **MyPersonalAI-owned microphone**
   - Dedicated `Talk to MyPersonalAI` control inside the app.
   - Explicit `RECORD_AUDIO` runtime permission.
   - Uses Android `SpeechRecognizer` directly; it does not depend on the keyboard microphone.
   - Clearly visible `Listening…`, `Processing…`, retry, and error states.
   - Stop/cancel support while listening.

3. **Unified text + voice pipeline**
   - Speech recognition returns plain text.
   - Spoken text is submitted through the same `ConversationViewModel.sendMessage()` path as typed text.
   - Existing contextual intent/tool behavior is therefore shared by both input modes.

4. **AI voice output**
   - Android `TextToSpeech` controller added for assistant responses.
   - Voice output can be enabled/disabled from the assistant header.
   - Assistant text remains visible even when voice output is disabled or unavailable.

5. **Assistant interaction behavior**
   - UI is prepared for confirmations such as `Opening Wi-Fi settings.` and failure responses such as `I couldn't find that.`.
   - Existing 5D contextual commands such as `open it` remain on the same conversation path.
   - Clarification behavior remains owned by the agent/contextual resolver rather than by the microphone layer.

## Deliberately deferred

The known Milestone 5D battery/device-information matching issue is not changed by this rebuild. The 5F objective is to authenticate the real MyPersonalAI voice input/output path without introducing a second command parser.

## Validation

Codespaces validation:

```bash
./gradlew test
./gradlew assembleDebug
```

The branch also contains a GitHub Actions workflow that runs tests, builds the debug APK, and packages the source ZIP when the 5F branch is pushed.

## Real-device voice validation

1. Install the 5F debug APK.
2. Do **not** use the keyboard microphone.
3. Tap `Talk to MyPersonalAI`.
4. Confirm the app itself changes to `Listening…`.
5. Say `open wifi settings`.
6. Confirm the spoken command becomes a MyPersonalAI conversation message and follows the existing settings intent path.
7. Say `open it` and confirm contextual routing.
8. Confirm MyPersonalAI displays and speaks its response.
9. Test Stop, permission denial, no speech, and recognition failure.
10. Retest normal typed settings commands for regression coverage.

## Branch

`milestone-5f-voice-interaction`

## Handoff

Do not merge into `main` until `./gradlew test`, `./gradlew assembleDebug`, installation, and real-device voice testing pass.
