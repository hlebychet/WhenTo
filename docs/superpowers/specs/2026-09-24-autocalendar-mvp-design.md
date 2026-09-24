# AutoCalendar MVP — Architectural Design

Date: 2026-09-24
Author: collaborative brainstorming decision

## 1. Goal

An Android app: the user selects a message (or several messages) from a messenger, shares it with the app, and the app automatically understands the meeting intent (title), date, time, duration, and location, shows the result for confirmation, and lets the user create a calendar event.

Key user requirement: **keep it simple**. Recognition is done by the local Gemini Nano model (Prompt API via AICore) — no cloud, no hand-written regex heuristics.

## 2. Context and Environment

- Target device for MVP: **Google Pixel 10 Pro** (supports Gemini Nano nano-v3, Prompt API, Structured Output API).
- Platform: Android, Kotlin, Jetpack Compose.
- MVP is built for personal use and testing; wide distribution to other devices comes later, but the architecture must allow swapping the parser without rework.
- Working directory is empty — a new project, no existing code.

## 3. User Scenario

1. A message arrives in a messenger, e.g.:
   - "Let's meet to discuss the mockup, on Thursday at 3 PM at Starbucks."
   - "Call about the apartment next Friday, at 7 PM."
   - Two or three messages in a row, some from the other person, some from the user — the user selects all of it and taps "Share".
2. The user taps "Share" → picks AutoCalendar.
3. The app parses the text locally (Gemini Nano, Structured Output).
4. A confirmation screen opens with the fields pre-filled: Title, Date, Time, Duration, Location.
5. The user corrects whatever is needed, taps "Create event".
6. The event is offered to the system calendar app (ACTION_INSERT) with pre-filled fields; the user picks a calendar and confirms.
7. Parses for which the user created a calendar event are saved to history; an event can be re-created from history.

## 4. Components

```
[Share Intent / text field] → [MeetingParser] → [ConfirmScreen] → [CalendarIntentBuilder] → ACTION_INSERT
                                     ↓
                              [HistoryRepository] → [HistoryScreen]
```

### 4.1 Input
- `ACTION_SEND` with type `text/plain` (`EXTRA_TEXT`) — receive "Share".
- Fallback manual text entry in a field on the main screen (editing/pasting from clipboard also works).
- Main screen: text field, "Parse" button, "History" button.

### 4.2 Recognition (`MeetingParser` — interface)
The interface defines the contract for future implementation swapping:

```kotlin
interface MeetingParser {
    suspend fun parse(request: ParseRequest): ParseResult
}
data class ParseRequest(val rawText: String, val today: LocalDate)
sealed class ParseResult {
    data class Success(val meeting: MeetingDraft) : ParseResult()
    data class Failure(val reason: ParseFailureReason) : ParseResult()
}
```

MVP implementation — `GeminiNanoParser`:
- Prompt: asks to extract a meeting entity from the text, explicitly states "Today: <date>" and the current date so relative expressions ("tomorrow", "on Thursday", "next Friday") are resolved correctly.
- Structured Output API (Alpha): `@Generable` + `@Guide` Kotlin class `DetectedMeeting` — title, date, time, duration, location.
- Time is returned as ISO strings (`YYYY-MM-DD`, `HH:mm`) — we do not ask the model to compute epoch, we parse it ourselves. The model does not know timezone and today's date — we pass these as strings in the prompt.
- Fallback: if Structured Output API is unavailable on the device (`isStructuredOutputFeatureAvailable() == false` or an error) — plain Prompt API with a "return JSON" instruction and manual string parsing.
- AICore quota errors (`BUSY`) — retries with exponential backoff, at most N attempts.

Result format `MeetingDraft`:
```kotlin
data class MeetingDraft(
    val title: String,
    val startDateTime: LocalDateTime,
    val durationMinutes: Int?,   // null → calendar suggests its own default
    val location: String?,       // null → empty field
)
```

### 4.3 Confirmation screen (`ConfirmScreen`)
- All fields editable (title, date, time, duration, location) — living language text inevitably gives "80% accuracy", correction is mandatory.
- Buttons: "Create event", "Cancel" (goes back, nothing is saved to history).

### 4.4 Creating a calendar event (`CalendarIntentBuilder`)
- Intent `ACTION_INSERT` with `data = Events.CONTENT_URI` (`content://com.android.calendar/events`).
- Extras: `EVENT_BEGIN_TIME`, `EVENT_END_TIME` (computed from start + duration; if no duration — then only begin), `_TITLE`, `EVENT_LOCATION`.
- `ActivityNotFoundException` (no system calendar) → user-facing error with a clear message.

### 4.5 History (`HistoryRepository`)
- Room table `parsed_meetings`: id, rawText, title, startMillis, endMillis, location, createdAt.
- A record is written **only** when the user created an event from the confirmed data (history shows what actually went to the calendar).
- History screen: list of records (title, date/time), tapping a record re-opens the confirmation screen with that data in order to re-create the event.

## 5. Tech Stack

- Kotlin, Jetpack Compose + Material 3, Navigation Compose.
- ML Kit GenAI Prompt API (`com.google.mlkit:genai:*`), Structured Output compiler (`com.google.mlkit:genai-schema-compiler:1.0.0-alpha1`) + KSP.
- Room (KSP).
- Coroutines.
- minSdk 26, targetSdk current at build time.

## 6. Testing

- Unit tests:
  - parsing ISO strings from `DetectedMeeting` into `MeetingDraft` (valid/garbage inputs);
  - `CalendarIntentBuilder` (extras, end-time computation, handling missing duration);
  - history repository (Room, in-memory).
- Fake `MeetingParser` in confirmation-flow tests: we do not run real Gemini Nano in unit tests.
- On-device verification: manual "Share" scenario with real messages.

## 7. Error Handling

- No model / Prompt API unavailable → clear message recommending to check AICore / device support.
- Model failed to parse ("Fail to parse") → show error, do not create the event.
- Model did not find date or time → treat as a failed parse: we do not create an event without a date, show an error.
- AICore quota (`BUSY`) → retries with backoff, then error.
- No calendar app → message.
- Empty/too short text → "add message text" message.

## 8. Future Expansion (out of MVP scope, but anticipated)

- Other devices: check `isPromptSupported()`/Gemini Nano versions, fallback parsers (heuristics or cloud API) — the `MeetingParser` interface already allows this.
- Meeting participants, a settings system (default duration), publishing to Play Store.
- None of this is implemented in the MVP — only the `MeetingParser` abstraction and clean boundaries remain.

## 9. Non-Goals of the MVP

- Cloud text parsing.
- Automatic event creation without confirmation.
- Voice message parsing.
- Handling participants and conversations longer than 4–5 messages.