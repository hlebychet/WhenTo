# Task 12 Report — Wire it together (navigation, DI, share intent handling)

**Status: DONE_WITH_CONCERNS** (concern = two brief code blocks did not compile verbatim; fixed minimally, documented below)

## What I implemented

All 11 steps of `task-12-brief.md`, in order:

1. **`AppContainer.kt` (new)** — DI container: Room database, `GeminiNanoParser`, `RoomParsedMeetingStore`, `AndroidCalendarLauncher`, `appScope` (`SupervisorJob + Dispatchers.Default`), `pendingSharedText`, `confirmRequest` flows, and `data class ConfirmRequest(draft, rawText)`. Verbatim.
2. **`AutoCalendarApp.kt`** — `lateinit var container` created in `onCreate`. Verbatim.
3. **`ui/AppNav.kt` (new)** — `Routes`, Navigation Compose `NavHost` (MAIN/CONFIRM/HISTORY), single `MutableStateFlow<ConfirmRequest?>` driving both share-entry navigation and history recall, and the three factory functions. History record is saved on `container.appScope.launch { container.store.add(...) }` inside `onEventSaved`, then `onDone()` (spec §4.5). `MeetingTextValidator` object wrapped as `{ MeetingTextValidator.validate(it) }`. `toDraft` import ABSENT (verified by grep).
4. **`MainActivity.kt`** — share-intent handling (`handleSharedText` on create + `onNewIntent`), `AppNav` wired to the three `build*ViewModel` helpers; history recall maps `item.toDraft()` + `item.rawText` into `ConfirmRequest`.
5. **`data/ParsedMeetingExt.kt` (new)** — `ParsedMeeting.toDraft(zone)` extension. Verbatim.
6. **`ui/main/MainScreen.kt`** — whole-file replacement: bound to `MainViewModel` (text/isLoading/error via `collectAsState`), History `TextButton` via `onOpenHistory`. Verbatim.
7. **`ui/confirm/ConfirmScreen.kt`** — whole-file replacement: bound to `ConfirmViewModel` (title/location/error via `collectAsState`), read-only start-time line, `Create Event` → `viewModel.onCreateClick()`, `Cancel` → `onCancel`. Verbatim except one line (see concern).
8. **`ui/confirm/ConfirmViewModel.kt`** — targeted addition only: `onTitleChange(String)` and `onLocationChange(String?)` inserted after `onDraftReady`. Diff confirms exactly +10 lines, every existing member untouched.

## Deviation from the brief (the concern)

The brief's code did not compile as written — two instances of Kotlin's "smart cast is impossible because the property is a delegated property" error (values from `by collectAsState()` cannot smart-cast). Exact compiler output from Step 9, first run:

```
e: file:///.../com/autocalendar/ui/AppNav.kt:56:44 Smart cast to 'String' is impossible, because 'pending' is a delegated property.
e: file:///.../com/autocalendar/ui/confirm/ConfirmScreen.kt:73:46 Smart cast to 'Long' is impossible, because 'startMillis' is a delegated property.
```

Minimal fixes (semantics unchanged), the smallest possible departure from the brief text:

- `AppNav.kt` MAIN composable: `val shared = pending; if (shared != null) { viewModel.onTextChange(shared); pendingSharedText.value = null }` (brief used `if (pending != null) { viewModel.onTextChange(pending) ... }`).
- `ConfirmScreen.kt`: `val millis = startMillis; if (millis != null) { Text(Instant.ofEpochMilli(millis)...) }` (brief used `if (startMillis != null) { Text(Instant.ofEpochMilli(startMillis)...) }`).

Everything else in those two files is character-for-character the brief's block (verified programmatically by extracting the brief's fenced blocks and diffing against the files: `AppContainer`, `AutoCalendarApp`, `MainActivity`, `ParsedMeetingExt`, `MainScreen` = VERBATIM MATCH; the two diffs above are the only divergences; the setters block is present verbatim).

## What I tested

- **Step 9 — compile:** `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:compileDebugKotlin` — first run FAILED with the two smart-cast errors above; after the two fixes: **BUILD SUCCESSFUL**.
- **Step 10 — full unit suite:** same JAVA_HOME prefix + `.\gradlew.bat :app:testDebugUnitTest` — **BUILD SUCCESSFUL**, all green: **35 tests, 0 failures, 0 errors** across 11 classes (CalendarLauncherTest 3, EventTimesTest 4, ParsedMeetingStoreTest 2, ParseResultTest 2, MeetingDraftMapperTest 5, MeetingTextValidatorTest 4, PlainDetectedMeetingAdapterTest 3, PromptFactoryTest 1, ConfirmViewModelTest 5, HistoryViewModelTest 2, MainViewModelTest 4). Counts read from `app/build/test-results/testDebugUnitTest/*.xml`.

## Files changed (exactly eight)

| File | Action |
|---|---|
| `app/src/main/java/com/autocalendar/AppContainer.kt` | created |
| `app/src/main/java/com/autocalendar/AutoCalendarApp.kt` | modified |
| `app/src/main/java/com/autocalendar/ui/AppNav.kt` | created |
| `app/src/main/java/com/autocalendar/MainActivity.kt` | modified |
| `app/src/main/java/com/autocalendar/data/ParsedMeetingExt.kt` | created |
| `app/src/main/java/com/autocalendar/ui/main/MainScreen.kt` | replaced |
| `app/src/main/java/com/autocalendar/ui/confirm/ConfirmScreen.kt` | replaced |
| `app/src/main/java/com/autocalendar/ui/confirm/ConfirmViewModel.kt` | +2 setters only |

`git status --porcelain` after `git add .` showed ONLY these eight (report/ledger in git-ignored `.superpowers/`). Nothing else touched: `MainViewModel`, `HistoryViewModel`, `HistoryScreen`, parser package, calendar package, Room store, `AutoCalendarDatabase`, `AndroidManifest.xml`, all gradle/build files, `libs.versions.toml` — untouched.

## Commit created

- **`59c5f09` `feat: wire navigation, DI and share intent handling`** (8 files changed, 298 insertions, 63 deletions) on `feat/autocalendar-mvp`.
- Working tree clean after commit (`git status --porcelain` empty).

## Self-review findings

- Blocks verbatim (programmatic block-vs-file diff), except: ConfirmViewModel = targeted +2 setters; AppNav/ConfirmScreen = the two documented smart-cast fixes.
- No other files changed; staged set exactly the eight briefed files.
- No emojis: all eight files verified ASCII-only by regex scan (`[^\x00-\x7F]` → none).
- AppNav `toDraft` import absent; `toDraft` used only in MainActivity (`item.toDraft()` with saved `item.rawText`, per ruling).
- Commit message exactly as prescribed; `git status` clean after commit.
- Full suite green (35/35).

## Concerns for the reviewer

1. **Brief did not compile verbatim** (the two smart-cast errors above). The fixes are mechanical and semantics-preserving, but the brief's "transcribe exactly" instruction was technically violated in those two spots — reviewer should confirm the fixes are acceptable.
2. Known parked deviation carried in from the Task 10/11 rulings (not this task's doing): date/time/duration on Confirm are read-only (spec §4.3 "all fields editable" not fully met); `HistoryScreen` accepts `onBack` but its body never invokes it (no BackHandler on the History route — the plan's host-level fix was not part of the brief's blocks).
3. Behavioral note (deliberate, per brief): after a successful event creation, `onDone()` sets `confirmRequest = null` and calls `finish()` — the confirm `LaunchedEffect` may briefly re-navigate before `finish()`; harmless in practice, noted for the on-device pass in Task 13.

---

# Task 12 Fix Report — navigation hardening (review Important finding)

**Status: DONE.** Review verdict: zero Criticals; the ONE Important finding (sticky one-shot `confirmRequest` + unconditional navigate) was fixed per the controller's exact ruling; the five Minors and two "cannot verify" items were already adjudicated in the ledger and were NOT touched.

## What I changed

Only `app/src/main/java/com/autocalendar/ui/AppNav.kt`, exactly the three ruled edits, every other byte of the file unchanged (verified with `git diff` — 1 file changed, 8 insertions, 1 deletion, the three hunks below and nothing else):

1. Added `import androidx.activity.compose.BackHandler`.
2. Guarded the navigation effect: `if (request != null && nav.currentDestination?.route != Routes.CONFIRM)` — prevents double-pushed CONFIRM entries after Activity recreation while already on CONFIRM.
3. Reworked the CONFIRM composer: `BackHandler { confirmRequest.value = null; nav.popBackStack() }` so system back also clears the one-shot request (fixes the stale draft re-direct), plus an `else { LaunchedEffect(Unit) { nav.popBackStack() } }` branch so a null-request route self-heals instead of showing a blank CONFIRM (process death).

No other file touched: MainScreen, ConfirmScreen, MainActivity, ConfirmViewModel, AppContainer, AutoCalendarApp, ParsedMeetingExt — untouched; no build/manifest changes.

## Verification

- **Compile:** `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:compileDebugKotlin` → **BUILD SUCCESSFUL** (2s, 17 actionable tasks).
- **Full unit suite:** `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:testDebugUnitTest` → **BUILD SUCCESSFUL** (6s) — **35 tests, 0 failures, 0 errors** (summed from `app/build/test-results/testDebugUnitTest/*.xml`, same 11 classes as before the fix).

## Commit

- **`aad458d` `fix: harden confirm navigation against recreation and back`** on `feat/autocalendar-mvp` — staged set was exactly `AppNav.kt` (checked `git status --porcelain` before committing); working tree clean after commit.

## Note

The destination guard also neutralizes the pre-existing concern #3 above (re-navigate racing `finish()`): while still on CONFIRM the effect no longer navigates again.
