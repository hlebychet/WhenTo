# On-device verification notes (AutoCalendar MVP)

Date: 2026-09-25
Device: Pixel 10 Pro, build installed via `:app:installDebug`.
Unit tests before/after: 47 tests, 0 failures, 12 classes.

## Task 13 Step 3: E2E happy path

Input (shared via SEND to the app): "Встреча в четверг в 15:00 обсудить макет у стабакса".

- Main screen shows the shared text.
- Tap "Extract Meeting" -> parse (on-device inference) -> Confirm screen with
  Title "Meeting at Starbucks", Location "Starbucks", "24 Sept 2026 15:00".
  (The model repeated "Starbucks" as the title; accepted as model quality for MVP.)
- Tap "Create event" -> Google Calendar event editor opens with prefilled
  Title/Start (Thu, 24 Sept 2026 15:00)/End (16:00); our app activity finishes.
- Tap "Save" in the calendar -> event persisted; calendar provider row
  `_id=769 title=Meeting at Starbucks dtstart=1790244000000 (24 Sep 2026 15:00 +05)
  dtend=1790247600000 eventLocation=Starbucks`.
- Reopen the app -> History shows "Meeting at Starbucks / 24 Sept 2026 15:00 /
  Starbucks"; tapping the record re-opens Confirm with those values.

Finding + fix: `queryIntentActivities` returned nothing for the calendar INSERT intent
because the manifest `<queries>` declared only `scheme="content"`; Google Calendar's
insert filter is declared by mime type `vnd.android.cursor.dir/event`. Added a
mimeType-declared `<queries>` entry (kept the scheme entry).

## Task 13 Step 4: relative dates and multi-message text

- "Позвони маме в следующую пятницу, в 19 часов":
  - Unfixed model output: "30 Sept 2026 19:00" (a Wednesday) for RU and EN alike -
    the beta AICore model ignores the passed "today" anchor for "next <weekday>".
  - Fix: `NextWeekdayDateCorrector` deterministically resolves an unambiguous
    "next <weekday>" (RU + EN) to that weekday of the following ISO week; wired into
    `GeminiNanoParser`. Unit coverage in `NextWeekdayDateCorrectorTest`.
  - After fix: "02 Oct 2026 19:00".
- English spec scenario "Call about the apartment next Friday at 7 PM": after fix
  -> "Apartment Call / 02 Oct 2026 19:00".
- Multi-message dialog (two speakers): -> single meeting "Meeting about project /
  29 Sept 2026 11:00" (Tuesday, correct).
- Also checked, all correct with the strengths the model does have: "завтра"
  -> next day, "сегодня" -> today, absolute date "5 октября" and bare weekday
  "в четверг" -> the coming Thursday.

## Task 13 Step 5: failure paths

- "ok": -> "Message is too short" (TOO_SHORT), no model call.
- "Встреча 5 октября" (date, no time): -> "No meeting date or time found"
  (MISSING_DATE_OR_TIME).
- Airplane mode enabled: full parse text succeeded offline -> Confirm
  "Meeting about mockup / 30 Sept 2026 16:00" (fully on-device inference).

## Environment facts captured during verification

- The Gemini Nano (nano-v3) model download only started flowing once the phone's
  corporate VPN (Riot VPN, llc.itdev.incy) was paused; AICore downloads may stall
  behind a routed tunnel. VPN was re-enabled afterwards.
- AICore model downloads complete independently of a client keeping `download()`
  active; after the download the parse resumed.