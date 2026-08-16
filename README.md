# MyPersonalAI

Production-oriented Kotlin Android personal AI assistant. Built in small,
approved milestones — see project instructions for the full architecture
and confirmation policy. This README is updated at the end of every
milestone.

## Status: Milestone 1 — project skeleton

What exists right now, and nothing more:
- A single Jetpack Compose screen showing "MyPersonalAI".
- No AI provider, no tools, no permissions, no network access, no
  persistence, no voice.
- `applicationId`: `com.jason.mypersonalai` (debug builds suffix `.debug`).
- `minSdk 26` / `targetSdk 34` / `compileSdk 34`, Kotlin 1.9.24, AGP 8.5.2,
  Java 17 toolchain, Gradle 8.7.

## One-time setup: materialize the Gradle wrapper jar

This repo ships `gradlew`, `gradlew.bat`, and
`gradle/wrapper/gradle-wrapper.properties`, but not the binary
`gradle-wrapper.jar` (binaries aren't hand-written). Generate it once,
from a machine with any Gradle installed (or Android Studio's bundled
Gradle) and network access:

```bash
cd MyPersonalAI
gradle wrapper --gradle-version 8.7
```

This drops `gradle/wrapper/gradle-wrapper.jar` into place. Commit it —
wrapper jars are meant to be checked in so the build is reproducible
without anyone needing Gradle preinstalled ever again. After this
one-time step, everyone (including CI) just runs `./gradlew ...`.

## Build & test commands

Run from the `MyPersonalAI/` root, on a machine with JDK 17 and the
Android SDK (`ANDROID_HOME`/`ANDROID_SDK_ROOT` set — Android Studio's
SDK Manager or `sdkmanager` command-line tools both work; Android
Studio itself is never required):

```bash
# 1. Compile
./gradlew compileDebugKotlin

# 2. Unit tests
./gradlew test

# 3. Lint (static checks)
./gradlew lint

# 4. Produce the debug APK
./gradlew assembleDebug
```

The APK lands at:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Install and test on a physical device

1. Enable Developer Options and USB debugging on the phone.
2. Connect via USB and confirm the device is visible:
   ```bash
   adb devices
   ```
3. Install:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. Launch "MyPersonalAI" from the app drawer and confirm the screen
   shows the title and subtitle text.

## Known limitations (milestone 1)

- No functionality beyond displaying the screen — this is expected.
- No app icon has been supplied yet (system default is used); a real
  icon can be added in a later, non-functional milestone if wanted.
- Wrapper jar must be generated once locally per the step above before
  `./gradlew` works at all.

## Security notes

- Zero permissions declared in `AndroidManifest.xml`.
- No secrets, keys, or tokens exist anywhere in this milestone.
- `.gitignore` excludes `local.properties`, build output, and any
  future `secrets.properties`.
