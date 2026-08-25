# MyPersonalAI — Milestone 5D Natural Language Patch

Overlay this ZIP onto the working Milestone 5C Settings project.

Purpose:
- improve natural-language settings routing;
- recognize configuration synonyms such as modify/configure/adjust/manage/customize;
- stop arbitrary battery/storage/network sentences from falling back to Device Info;
- add deterministic intent-resolution tests;
- prefer general notification settings on Android 13+.

Workflow: unzip into the repository, remove the ZIP, run `./gradlew assembleDebug`, install/test the APK. Do not merge/stage/push until phone testing passes.
