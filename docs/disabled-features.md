# Disabled features (MVP)

Decision on 2026-09-26: the MVP flow drops the Confirm screen as the primary
post-parse step and stops passing a location to the calendar editor. The system
calendar editor already serves as the review/confirmation surface, and location
is not needed for MVP. The code is kept in the tree so both features can be
restored quickly.

## Disabled: Confirm screen

Today the share flow and the history re-create flow open the calendar editor
directly with the parsed fields (title, start, duration) and write a history
entry at that point. If the calendar launcher fails (no calendar app installed),
the app falls back to the Confirm screen so the user can retry.

- Gate: `MainActivity.launchEditedCalendar()` (shared from `onDraftReady`
  in `buildMainViewModel()` and from `onSelect` in `buildHistoryViewModel()`).
- Restore: point those two callbacks back at
  `container.confirmRequest.value = ConfirmRequest(draft, rawText, finishOnDone)`
  (the old wiring is recoverable from git history of `MainActivity.kt`).
- The Confirm screen, `ConfirmViewModel`, its route and its unit tests remain
  in the tree and stay green, so restoring is a wiring change only.

## Disabled: location in calendar

The parser/model/prompt still extract a location (schema untouched), but the
value is never written to the calendar intent.

- Gate: `CalendarIntentBuilder.kt`, commented-out
  `event.location?.let { intent.putExtra("eventLocation", it) }` line.
- Restore: uncomment that line. Prompt and mapping already carry the location
  through, so no other change is needed.

## Notes

- History entries still accept a `location` value in the data model; the flow
  simply passes null today.
- No schema or prompt changes were made, so re-enabling either feature needs no
  retraining or prompt work.