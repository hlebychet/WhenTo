# Contributing to WhenTo

Developer guide: how to build, test and work on the codebase. End users start
at the [README](README.md).

## Project layout

- `app/` — the Android application (single Activity + Jetpack Compose).
- `docs/` — spec, plan and verification notes (`docs/superpowers/specs/`,
  `docs/superpowers/plans/`, `docs/verification-notes.md`,
  `docs/disabled-features.md`).

## Prerequisites

- JDK 21. On Windows, use the JDK bundled with Android Studio:
  `C:\Program Files\Android\Android Studio\jbr`. System JDK 24/25 is too new
  for Gradle 8.13. On Linux/macOS, install a JDK 21 (e.g. Temurin) and set
  `JAVA_HOME` to it.
- Android SDK. Point Gradle at it either with `local.properties`
  (`sdk.dir=...`) or the `ANDROID_HOME` environment variable.
- No system Gradle is needed — always use the wrapper.

## Build

From the project root:

```powershell
# Windows
.\gradlew.bat :app:assembleDebug
```

```bash
# Linux / macOS
./gradlew :app:assembleDebug
```

Artifacts land in `app/build/outputs/apk/` with names like
`WhenTo-v0.1.0-debug.apk`. A release APK requires a signing key:

- Locally: create `key.properties` next to `build.gradle.kts` with
  `storeFile`, `storePassword`, `keyAlias`, `keyPassword` (the `.jks` file and
  `key.properties` are git-ignored). Without it, the release build falls back
  to the debug key, which is fine for personal installs but not for public
  distribution.
- CI: GitHub Actions reads the same `key.properties` from repository secrets
  if they are configured.

## Testing

JVM unit tests cover parsing, validation, date handling, calendar intent
building, view models and history storage:

```powershell
.\gradlew.bat :app:testDebugUnitTest          # full suite
.\gradlew.bat :app:testDebugUnitTest --tests "com.autocalendar.ui.main.MainViewModelTest"
```

On-device verification notes: `docs/verification-notes.md`.

## Architecture

MVVM over three small seams, so core logic is pure Kotlin and unit-testable
without Android:

- `MeetingParser` — text → `MeetingDraft` (implemented by `GeminiNanoParser`,
  ML Kit GenAI on AICore).
- `ParsedMeetingStore` — event history persistence (Room).
- `CalendarLauncher` — ACTION_INSERT into the system calendar.

Parsing detail: the model returns meeting fields as JSON; a deterministic
pass (`NextWeekdayDateCorrector`) re-derives "next <weekday>" dates (RU and
EN), because the beta model resolves relative dates unreliably.

## Releases

Releases are automated: pushing a tag like `v0.1.0` triggers
`.github/workflows/release.yml`, which builds the release APK on GitHub and
creates a GitHub Release with the APK attached. Release notes are edited in
that workflow file.

## Workflow notes

- `main` is stable; feature work happens in a git worktree on a feature
  branch, then merges locally and is pushed. See `AGENTS.md`.
- Code, docs and commit messages are English (chat may be in Russian);
  typographic punctuation only, no emojis.