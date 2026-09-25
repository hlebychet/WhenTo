# Task 11 report — History screen

**Status: DONE** — all 7 steps complete. The controller re-ruled the blocked test line; ledger ruling superseded: test line now uses `vm.items.first { it.isNotEmpty() }` (test-only deviation, plus the required `import kotlinx.coroutines.flow.first`).

## What was implemented

Three files created per the brief and committed as `ce3aa2e`:

- `app/src/main/java/com/autocalendar/ui/history/HistoryViewModel.kt` — verbatim from brief Step 3.
- `app/src/main/java/com/autocalendar/ui/history/HistoryScreen.kt` — verbatim from brief Step 4.
- `app/src/test/java/com/autocalendar/ui/history/HistoryViewModelTest.kt` — verbatim from brief Step 1 except the single confirmed test deviation: `vm.items.value` → `vm.items.first { it.isNotEmpty() }`, plus the required import `kotlinx.coroutines.flow.first`.

Stale files from the crashed prior attempt: the stale test file was overwritten; the two stale main files (`HistoryViewModel.kt`, `HistoryScreen.kt`) were deleted first — the stale `HistoryViewModel.kt` was broken (`e: HistoryViewModel.kt:25:29 Operator call 'component1()' returns 'ParsedMeeting', but 'List<ParsedMeetingEntity>' is expected.`) and blocked `:app:compileDebugKotlin`, preventing the prescribed RED from being observed — then written verbatim from the brief. Final content of all three files is 100% spec-conformant; nothing of the stale versions was reused.

## TDD evidence

### RED (Step 2) — confirmed

Command:

```
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:testDebugUnitTest --tests "com.autocalendar.ui.history.HistoryViewModelTest"
```

First attempt failed at `:app:compileDebugKotlin` with the stale file's error above (not the expected failure). After deleting the two stale main files, re-ran the same command:

```
e: file:///.../app/src/test/java/com/autocalendar/ui/history/HistoryViewModelTest.kt:33:18 Unresolved reference 'HistoryViewModel'.
e: file:///.../app/src/test/java/com/autocalendar/ui/history/HistoryViewModelTest.kt:42:18 Unresolved reference 'HistoryViewModel'.
...
> Task :app:compileDebugUnitTestKotlin FAILED
BUILD FAILED in 2s
```

Expected failure observed: `HistoryViewModel` undefined. RED confirmed.

### Implementation (Steps 3–4)

Both main files written verbatim. `:app:compileDebugKotlin` then succeeded (observed as part of the Step 5 run: `> Task :app:compileDebugKotlin` completed, no `e:` diagnostics).

### Step 5 (GREEN) — initially failed with the original `first()` ruling, then passed after the re-ruling

First run with the ruled `vm.items.first()` (same command): `2 tests completed, 1 failed`, `expected:<[Second, First]> but was:<[]>` in `items are exposed from the store` (`select re-emits the tapped item` PASSED). Stopped and reported NEEDS_CONTEXT.

Controller re-ruled the line to `vm.items.first { it.isNotEmpty() }` (test-only; import `kotlinx.coroutines.flow.first` already present). After applying:

```
BUILD SUCCESSFUL in 3s
26 actionable tasks: 3 executed, 23 up-to-date
```

Test report XML: `<testsuite name="com.autocalendar.ui.history.HistoryViewModelTest" tests="2" skipped="0" failures="0" errors="0">` — **2 PASS**.

Re-run immediately before commit: `BUILD SUCCESSFUL`, same XML `tests="2" failures="0" errors="0"` (task `:app:testDebugUnitTest UP-TO-DATE`, identical inputs).

### Step 6 — compile

`$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:compileDebugKotlin` → `BUILD SUCCESSFUL in 1s` (17 tasks, all up-to-date; `:app:compileDebugKotlin` executed clean in the GREEN run above with no `e:` diagnostics).

## Why the original `first()` ruling failed (recorded for the ledger)

`vm.items.first()` does not "await the seeded emission". `first()` on a `StateFlow` returns the value visible at subscription time; `StateFlow.collect` emits its current value synchronously. With the plan's `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())` and no prior subscriber, the current value at that instant is the initial `emptyList()` — and `first()` returns it without ever suspending, so the sharing coroutine (which would pull `[Second, First]` from `FakeStore`) never gets a chance to run. Empirically `vm.items.first()` yielded byte-for-byte the same result as `vm.items.value`: `[]`.

## Ruling applied (controller-confirmed correction)

```kotlin
assertEquals(listOf("Second", "First"), vm.items.first { it.isNotEmpty() }.map { it.title })
```

Test-only; VM and HistoryScreen stay verbatim. `first { it.isNotEmpty() }` subscribes (starting `WhileSubscribed` sharing), suspends, lets the test dispatcher run the sharing coroutine, and resumes when the seeded store value reaches `items`. No hang risk: initial `[]` differs from the seeded list, so exactly one emission satisfies the predicate. The import `kotlinx.coroutines.flow.first` is present (line 6), alongside the existing `MutableStateFlow` import.

## Files changed

- `app/src/main/java/com/autocalendar/ui/history/HistoryViewModel.kt` (created, verbatim)
- `app/src/main/java/com/autocalendar/ui/history/HistoryScreen.kt` (created, verbatim)
- `app/src/test/java/com/autocalendar/ui/history/HistoryViewModelTest.kt` (created, verbatim + confirmed `first { it.isNotEmpty() }` deviation + `first` import)

No other paths touched. No build files, no `libs.versions.toml`, no other task's code; working tree clean after the commit.

## Commit (Step 7)

Exact prescribed commands run from the worktree root:

```
git add app/src/main/java/com/autocalendar/ui/history app/src/test/java/com/autocalendar/ui/history
git commit -m "feat: history screen"
```

Output:

```
[feat/autocalendar-mvp ce3aa2e] feat: history screen
 3 files changed, 154 insertions(+)
 create mode 100644 app/src/main/java/com/autocalendar/ui/history/HistoryScreen.kt
 create mode 100644 app/src/main/java/com/autocalendar/ui/history/HistoryViewModel.kt
 create mode 100644 app/src/test/java/com/autocalendar/ui/history/HistoryViewModelTest.kt
```

**Commit: `ce3aa2e` — feat: history screen.** `git show --stat` confirms exactly the three history files; `git status --porcelain` afterwards: empty (clean tree; `.superpowers/` is git-ignored and not in the commit).

## Self-review findings

- Signatures match the brief exactly: `HistoryViewModel(store, onSelect: (ParsedMeeting) -> Unit)`, `val items: StateFlow<List<ParsedMeeting>>`, `fun select(item: ParsedMeeting)`, `HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit)` — no extra parameters, defaults, or members. Task 12 wiring will match.
- Only deviation from verbatim brief text: the confirmed test line `vm.items.first()` → `vm.items.first { it.isNotEmpty() }` plus import `kotlinx.coroutines.flow.first`; everything else in all three files is character-identical to the plan (reread file-by-file against the brief before commit).
- UI strings exactly `History` and `No saved meetings yet.`
- No emojis anywhere (all files ASCII-only punctuation).
- `onBack` intentionally unused in `HistoryScreen` (verbatim plan; wired in Task 12) — no compiler error/warning.
- Commit contains only the three history files: no `.superpowers/`, no build files, no other task's code.

## Concerns

1. None blocking. Two process notes for the ledger: (a) the original `first()` ruling was falsified and superseded by the controller-confirmed `first { it.isNotEmpty() }`; (b) the stale out-of-spec `HistoryViewModel.kt` from the crashed prior attempt blocked the first RED run with its own compile error — deleted (it was declared out of spec) before observing the clean `Unresolved reference 'HistoryViewModel'` RED.
2. Focused-test re-run before commit reported `:app:testDebugUnitTest UP-TO-DATE` (Gradle cache, identical inputs); the authoritative GREEN result is the executed run: `tests="2" failures="0" errors="0"`.
