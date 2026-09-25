# Final Review Fix Wave - Report

- Branch: `feat/autocalendar-mvp` (worktree `.worktrees/autocalendar-mvp`)
- Commit: `05c835c6ddf87b9cebf93d22f3d0a5f6b6ec5ee1` (`05c835c`) - message `fix: address final review findings`
- Scope: 17 paths (16 modified + 1 new file), single commit, nothing else staged.

## Per-finding resolution

| Finding | Status | Note |
| --- | --- | --- |
| C1 parser can never succeed | fixed | `plainJsonExtraction` now uses `model.generateContent(prompt)` + `response.candidates.firstOrNull()?.text ?: return null`; reflection `when (response)` block and the `?:` on a non-nullable value deleted; dead `val prompt = ...` removed from `runExtraction`. |
| C2 package visibility kills calendar pre-check | fixed | `<queries>` (action `android.intent.action.INSERT`, scheme `content`) added as a direct child of `<manifest>`, placed before `<application>`; no comments. |
| I1 wrong calendar title key | fixed | Builder puts `CalendarContract.Events.TITLE`; `CalendarLauncherTest` asserts the same key. |
| I3 empty history-save test | fixed | `create event saves to history on success` now verifies `onEventSaved` fires once with title, `beginMillis == startMillis` (captured before click), `endMillis = begin + 30*60000`, location; removed dead `FakeParser` and the `ParseResult` / `MeetingParser` / `CompletableDeferred` / `EventToSave` imports. |
| I4 raw enum names shown to user | fixed | New `ui/ErrorMessages.kt` (pure Kotlin, no Android imports) maps all 7 `ParseFailureReason` and both `LaunchFailureReason` members; wired into `MainViewModel` (2 sites), `ConfirmViewModel` (1 site) and the 3 test assertions. |
| I5 .gitignore regression | fixed | `.worktrees/` and `.superpowers/` appended to the tracked root `.gitignore`; no existing entries removed. |
| I6 cancellation treated as quota | fixed | `parse()` catch rethrows `kotlinx.coroutines.CancellationException` first (import added); quota-retry logic untouched. |
| M1 stale share-text reprocessing | fixed | `onCreate` calls `handleSharedText` only when `savedInstanceState == null`; `onNewIntent` calls `setIntent(intent)` first. |
| M2 recall path closes the app | fixed | `ConfirmRequest.finishOnDone: Boolean = true` added; share flow `onDone` finishes only when `finishOnDone`; history recall constructs with `finishOnDone = false`. |
| M3 fire-and-forget store.add can crash appScope | fixed | `container.store.add(...)` wrapped in try; `CancellationException` rethrown, other exceptions swallowed; `onDone()` still invoked after the launch block. |
| M4 magic status codes | fixed | Private companion constants `STATUS_UNAVAILABLE=0`, `STATUS_NEEDS_DOWNLOAD=1`, `STATUS_DOWNLOADED=2`, `STATUS_AVAILABLE=3` replace 0/1/2/3; behavior identical. |
| M5 dead code | fixed | `ConfirmViewModel`: removed `ParsedMeetingStore` import and the `typealias TextValidator` (`rawText` parameter of `onDraftReady` kept). `AndroidCalendarLauncher`: removed `ResolveInfo` / `Activity` imports and the `context is Activity` branch - always `FLAG_ACTIVITY_NEW_TASK` + `startActivity`, `ActivityNotFoundException` catch kept. |
| M7 ZoneId per recomposition | fixed | `HistoryScreen` now uses `val zone = remember { ZoneId.systemDefault() }` (plus the `remember` import); nothing else in the file changed, `onBack` kept. |
| M9 JSON adapter robustness | fixed | Case-insensitive opening-fence strip, trailing-fence strip, whole-text parse first (identical behavior for plain objects), then first balanced `{...}` depth scan; 3 new tests (prose preamble, uppercase ```JSON fence, plain object input). |
| M10 blank title passes | fixed | `onCreateClick` rejects null-or-blank title with `_error.value = "Title cannot be empty"` and returns before launching. |

Findings not present in this fix list (I2, M6, M8 and any others from the review): n/a for this wave - not provided in the fix list, therefore not touched.

## Compile-driven adaptations

None required. The verified API facts held exactly: `generateContent(prompt)` returned a non-nullable `GenerateContentResponse`, and `response.candidates.firstOrNull()?.text` compiled on the first `:app:compileDebugKotlin` run (BUILD SUCCESSFUL, 0 warnings/errors from `GeminiNanoParser.kt`). No version bumps, no signature drift, no fallback logic needed.

Minor judgment calls, all inside listed files:
- `<queries>` placed before `<application>` (both positions are siblings under `<manifest>`; this is the conventional order).
- In `ConfirmViewModel` the pre-existing unused imports `CalendarLaunchOutcome`, `EventToSave` and `LaunchFailureReason` (all referenced only via fully-qualified names) were removed together with the listed `ParsedMeetingStore` import - same M5 dead-code class, zero behavior change. Same for the unused `EventToSave` import in `ConfirmViewModelTest`, per the "test file compiles clean" bar.
- Existing pre-existing comments in untouched test methods were left as-is (minimal diff).

## Verification

1. `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:compileDebugKotlin` -> BUILD SUCCESSFUL.
2. `.\gradlew.bat :app:testDebugUnitTest` -> BUILD SUCCESSFUL. Totals: **38 tests, 0 failures, 0 errors, 0 skipped, 11 classes** (prior suite: 35 tests / 11 classes; +3 from the new adapter tests - the history-save test already existed as an empty test and is now filled).
   - com.autocalendar.calendar.CalendarLauncherTest: 3
   - com.autocalendar.calendar.EventTimesTest: 4
   - com.autocalendar.data.ParsedMeetingStoreTest: 2
   - com.autocalendar.domain.ParseResultTest: 2
   - com.autocalendar.parser.MeetingDraftMapperTest: 5
   - com.autocalendar.parser.MeetingTextValidatorTest: 4
   - com.autocalendar.parser.PlainDetectedMeetingAdapterTest: 6
   - com.autocalendar.parser.PromptFactoryTest: 1
   - com.autocalendar.ui.confirm.ConfirmViewModelTest: 5
   - com.autocalendar.ui.history.HistoryViewModelTest: 2
   - com.autocalendar.ui.main.MainViewModelTest: 4
3. `.\gradlew.bat :app:assembleDebug` -> BUILD SUCCESSFUL (manifest with `<queries>` processed by `processDebugMainManifest` and packaged).
4. `git status` after commit: clean - no unstaged changes, no untracked files, no build artifacts, no `.superpowers/` files staged (`.superpowers/` and `.worktrees/` are now ignored by the committed `.gitignore`).

Remaining pre-existing compile warnings only: `ExperimentalCoroutinesApi` opt-in notices for `advanceUntilIdle` in the two ViewModel test files (present before this wave, not a failure).

## Addendum - re-review minors (second commit)

- Commit: `1f02b66e9148e578b3e62365e86e4ad3f5420584` (`1f02b66`) - message `fix: close re-review minors`
- Scope: 3 files modified (no new files), single commit.

What changed:

1. `PlainDetectedMeetingAdapter` brace scan hardened. The fallback now tracks JSON string-literal state (including escaped quotes) while scanning, so braces inside strings are never counted, and it tries each balanced `{...}` span in order, returning the first one `JSONObject` can parse (null only if none parses) instead of blindly slicing from the first `{`. Whole-text parse for already-object input is untouched, so that path behaves identically. Added a regression test: `stray braces in prose are skipped in favor of the real object` (`See {details} here: {...}`), which fails against the previous implementation.
2. `create event saves to history on success` now collects saved events into a list (`savedEvents += it`), asserts `assertEquals(1, savedEvents.size)` (double-`onEventSaved` regression now fails), then checks the single element's fields.
3. New test `blank title is rejected without launching`: `onDraftReady` with a whitespace title, then `onCreateClick()` sets `vm.error.value == "Title cannot be empty"` and `launcher.lastEvent` stays null (M10 coverage).

Verification before this commit:

1. `.\gradlew.bat :app:compileDebugKotlin` -> BUILD SUCCESSFUL.
2. `.\gradlew.bat :app:testDebugUnitTest` -> BUILD SUCCESSFUL. Updated totals: **40 tests, 0 failures, 0 errors, 0 skipped, 11 classes** (up from 38; +1 adapter regression test, +1 M10 test).
   - com.autocalendar.calendar.CalendarLauncherTest: 3
   - com.autocalendar.calendar.EventTimesTest: 4
   - com.autocalendar.data.ParsedMeetingStoreTest: 2
   - com.autocalendar.domain.ParseResultTest: 2
   - com.autocalendar.parser.MeetingDraftMapperTest: 5
   - com.autocalendar.parser.MeetingTextValidatorTest: 4
   - com.autocalendar.parser.PlainDetectedMeetingAdapterTest: 7
   - com.autocalendar.parser.PromptFactoryTest: 1
   - com.autocalendar.ui.confirm.ConfirmViewModelTest: 6
   - com.autocalendar.ui.history.HistoryViewModelTest: 2
   - com.autocalendar.ui.main.MainViewModelTest: 4
3. `git status`: clean after commit - only the 3 intended files were staged; no build artifacts, no `.superpowers/` files, no other changes.

All three re-review minors are closed; nothing parked.
