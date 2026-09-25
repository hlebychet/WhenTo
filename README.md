# WhenTo

Local-first Android app that extracts a meeting from a messenger message you
share with it, shows the result for confirmation, and creates a calendar
event. All parsing runs on-device via Google Gemini Nano (ML Kit GenAI on
AICore) — no network, no cloud, no text leaves the device.

## What it does

1. You share one or more selected messages from a messenger ("Let's meet to
   discuss the mockup, on Thursday at 15:00 at Starbucks", a two-line chat
   excerpt, etc.) into WhenTo.
2. The app parses the text locally and fills a confirmation screen: title,
   date, time, duration, location.
3. You review and tap "Create event" — the system calendar opens with the
   event pre-filled.
4. A history of created events is kept in the app; tapping a record re-opens
   the confirmation screen to re-create the event.

## Requirements

- Android 8.0+ (minSdk 26).
- The on-device parser needs Gemini Nano via the ML Kit GenAI Prompt API on
  AICore. At the time of writing this is available on a Pixel 10 Pro after:
  - AICore experimental enrollment,
  - the nano-v3 model download (may take a few minutes; a routed VPN can
    stall it — pause the VPN while it completes).
- No other hardware or account is required; the app itself needs no Google
  account.

## Architecture

- Single Activity + Jetpack Compose, MVVM (`MainViewModel`,
  `ConfirmViewModel`, `HistoryViewModel`).
- Three small seams keep the logic testable without Android:
  - `MeetingParser` — text → `MeetingDraft` (implemented by
    `GeminiNanoParser`),
  - `ParsedMeetingStore` — history persistence (Room),
  - `CalendarLauncher` — ACTION_INSERT into the system calendar.
- All parsing/validation/date logic is pure Kotlin unit-tested on the JVM.

Parsing detail: the model is asked to return meeting fields as JSON; a
deterministic pass (`NextWeekdayDateCorrector`) re-derives "next <weekday>"
dates (RU and EN) from the current date, because the beta model resolves
relative dates unreliably.

## Building

Windows prerequisites: set `JAVA_HOME` to the bundled Java 21 before any
Gradle command (the system JDK 24/25 is too new for Gradle 8.13):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

Commands (from the project root):

- Debug APK: `.\gradlew.bat :app:assembleDebug`
- Unit tests: `.\gradlew.bat :app:testDebugUnitTest`
- Install to a connected device: `.\gradlew.bat :app:installDebug`

## Testing

- JVM unit tests cover parsing, validation, date handling, calendar intent
  building, view models, and history storage (47 tests, 12 classes).
- On-device verification results are recorded in
  [docs/verification-notes.md](docs/verification-notes.md).

## Project layout

- `docs/superpowers/specs/` — design spec (requirements, architecture).
- `docs/superpowers/plans/` — implementation plan (13 tasks, TDD).
- `docs/verification-notes.md` — on-device test results.
- `app/` — the Android application.

## Known limitations (MVP)

- Date/time/duration are read-only on the confirmation screen; edit them in
  the calendar editor after "Create event".
- Structured Output (alpha) is not used; the plain-JSON parse path is active.
- Relative dates beyond "next <weekday>" (e.g. "the day after tomorrow",
  "in a week") follow whatever the model produces.
- The model sometimes repeats the location as the title; correct it in the
  calendar editor before saving.