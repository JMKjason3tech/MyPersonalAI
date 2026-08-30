# MyPersonalAI — Milestone 6B Voice Fix v4

## Problem fixed
The 6B voice implementation regressed both online and offline microphone behavior.

## Changes
- Online devices now use Android's normal SpeechRecognizer when a validated network is available.
- Offline devices prefer Android's on-device SpeechRecognizer when supported.
- EXTRA_PREFER_OFFLINE is no longer forced to true while online.
- The recognizer is recreated for each listening session so stale/busy recognizers do not persist between attempts.
- Permission flow no longer ends at the permission callback; the user can retry immediately after granting permission.
- Better handling for network, server, busy, timeout, and unavailable-recognizer errors.
- Recognized speech still flows through the existing `onTextResult` conversation pipeline.

## Important offline requirement
True offline recognition still requires an Android speech service with an installed on-device language model. If the phone does not provide one, MyPersonalAI reports that explicitly instead of claiming offline speech works.

## Test
Run:

```bash
./gradlew test
```

Then build/install and test the microphone first while online, then with Wi-Fi/mobile data disabled.
