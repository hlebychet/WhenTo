# SDD ledger — plan: docs/superpowers/plans/2026-09-24-autocalendar-mvp.md

## Environment (established before Task 1)

- OS: Windows 11, shell pwsh. All git/gradle commands run by implementers in the worktree `D:\Documents\AIProjects\autoCAlendar\.worktrees\autocalendar-mvp` (branch `feat/autocalendar-mvp`).
- Android SDK at `C:\Users\hlebychet\AppData\Local\Android\Sdk` (platform android-36, android-36.1; build-tools 35.0.0, 36.1.0). `local.properties` has `sdk.dir` (git-ignored).
- System JAVA_HOME points at JDK 24/25 — too new for Gradle 8.13. **Every gradle command must first run `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`** (JDK 21.0.7 bundled with Android Studio). This is a mandatory part of each implementer dispatch.
- No system `gradle` on PATH and no wrapper yet. The wrapper is bootstrapped in Task 1 (see ruling below for the exact mechanism agreed with the environment).
- Android Studio JBR java.exe: `C:\Program Files\Android\Android Studio\jbr\bin\java.exe`.
- Repo baseline commit (main): `6a50897`. Feature branch `feat/autocalendar-mvp` starts there; worktree HEAD originally `6a50897`, then fast-forwarded to `94b084f` when `AGENTS.md` was added (both `main` and the worktree now contain it; `AGENTS.md` is the repo-guidance file).

## Preflight conflict scan

Pairs of tasks sharing a file/interface, or task text disagreeing with itself:

| Tasks | Shared surface | Finding |
|---|---|---|
| 1 → 2–13 | Project scaffold, libs.versions.toml, manifest, gradle files | Task 1 creates the scaffold every later task builds on. No contradiction found. |
| 2 → 3,4,8 | `ParseResult`, `ParseFailureReason`, `ParseRequest`, `MeetingDraft`, `MeetingParser` | Consumed verbatim downstream; plan text matches. Clean. |
| 3 → 8,12 | `MeetingTextValidator` object | Used as `MeetingTextValidator` constructor arg / import in Tasks 8 & 12. Clean. |
| 4 → 7 | `DetectedMeeting` (with `@Generable`/`@Guide`), `MeetingDraftMapper`, `PromptFactory` | Task 7 uses all three verbatim. Clean. |
| 4 internal | Task 4 test vs code | Test imports `java.time.LocalDateTime` (used); code files self-consistent; PromptFactory test lives in its own file `PromptFactoryTest.kt`. Clean. |
| 5 → 9,10 | `EventTimes.beginMillis/endMillis` | Task 9 `CalendarIntentBuilder` and Task 10 tests consume. Constant `1_790_337_600_000L` = 2026-09-25 15:00 +03:00 verified. Clean. |
| 6 → 10,11,12 | `ParsedMeeting`, `NewParsedMeeting`, `ParsedMeetingStore`, `RoomParsedMeetingStore`, `AutoCalendarDatabase` | Tasks 10–12 consume; Task 12 adds `toDraft` extension. Clean. |
| 7 internal | Parser (GeminiNanoParser) | `isQuotaError` uses `GenAiException.errorCode.name`; structured-path call `generateTypedContentRequest(base, DetectedMeeting::class)` is beta API — flagged in plan as subject to drift; Task 7 Step 4 mandates adapting to the current API and documenting. Acceptable, keep-rule for `DetectedMeeting` in proguard (Task 1 Step 7). Clean. |
| 8 → 12 | `MainViewModel(parser, validator, onDraftReady: (MeetingDraft, String) -> Unit)` | Task 12 `createMainViewModel` matches. Clean. |
| 9 → 10 | `CalendarIntentBuilder`, `EventToSave`, `toCalendarIntent()`, `CalendarLaunchOutcome`, `AndroidCalendarLauncher` | Task 10 ConfirmViewModel consumes. `CalendarLaunchBehaviorTest` separate file, no dup. Clean. |
| 10 → 11,12 | `ConfirmViewModel`, `ConfirmScreen(viewModel, onCancel)` | Matches AppNav usage in Task 12. Clean. |
| 11 → 12 | `HistoryViewModel(store, onSelect: (ParsedMeeting) -> Unit)`, `HistoryScreen(viewModel, onBack)` | Matches Task 12 usage. Clean. |
| 12 internal | AppNav wiring | Rewritten to single `MutableStateFlow<ConfirmRequest?>`; `startRoute`/`openConfirm` removed. self-consistent. Clean. |

Scan result: **clean** — no conflicts requiring preflight rulings beyond the wrapper-bootstrap mechanics noted here.

## Rulings

- **Ruling (environment): Gradle wrapper bootstrap.** No system gradle exists and Task 1's scaffold step assumes a wrapper. Mechanism for Task 1: download Gradle 8.13 distribution zip to the temp dir, unpack, run `gradle wrapper --gradle-version 8.13` from the worktree root with JAVA_HOME=JBR JDK 21, then remove the unpacked distro from the temp dir. This produces the committed `gradlew`, `gradlew.bat`, `gradle/wrapper/*`. — Why: no other gradle binary is available and the plan pins Gradle 8.13. — Cost if wrong: a one-off environment step, trivially replaced by a different distribution source.
- **Ruling (environment): gradle daemon must run on JDK 21.** Every implementer dispatch includes the JAVA_HOME prefix line; deviation risks daemon startup failure under JDK 24/25 — cheap to notice and correct.
- **Ruling (plan defect, Task 11 test): `vm.items.value` reads the initial empty state.** The plan's Step 1 test asserts `vm.items.value.map { it.title }` immediately after constructing `HistoryViewModel`, but the prescribed `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())` does not collect the upstream until a subscriber appears, so `.value` is `emptyList()` forever at that instant; the test cannot pass as written. — Why: the plan's implementation code is otherwise sound and the test's intent is to verify store → items exposure and order. — Cost if wrong: the test passes spuriously or a reviewer disputes the deviation; both are trivially re-ruled.
- **Ruling SUPERSEDED (the line above).** `vm.items.first()` does NOT fix it: `first()` on a StateFlow returns the current value synchronously (emptyList) without yielding a beat for the `stateIn` sharing coroutine to start, so it fails identically to `.value`. Corrected fix (test-only, same intent, VM verbatim): `vm.items.value` → `vm.items.first { it.isNotEmpty() }`, which suspends until the sharing coroutine (auto-advanced by runTest) pushes the seeded store data. Requires `import kotlinx.coroutines.flow.first` (flow extension; not provided by `kotlinx.coroutines.test.*`). — Cost if wrong: the test passes only if the upstream emits non-empty; still proves store→items wiring.
- **Ruling (convention): no emojis anywhere.** The human banned emojis from the whole repo (code, UI strings, comments, docs, commit messages). Arrows (→ ↓) and en/em dashes are typographic punctuation, allowed. The rule now lives in `AGENTS.md` → Conventions; implementer/reviewer dispatches must enforce it (the plan/spec copies already had the `👍` example removed on both branches).
- **Note (Windows quirk): SDD skill scripts mis-resolve the plan workspace.** Under Git for Windows, `git rev-parse --show-toplevel` prints `D:/…` while `pwd -P` prints `/d/…`, so `sdd-workspace`'s plan-id prefix-strip fails and it creates a stray `…-mvp-plans/` directory next to the canonical one. Canonical workspace = `.worktrees/autocalendar-mvp/.superpowers/sdd/2026-09-24-autocalendar-mvp/`. If `task-brief`/`review-package` print a `…-plans` path, move/copy the artifact into the canonical dir and delete the stray one. pass an explicit OUTFILE to `task-brief` to avoid the issue.

## Model roles (configured in repo-root opencode.json)

Provider `opencode`, IDs verified from the live registry `models.opencode.ai/api.json`.

**CURRENT (2026-09-25): all roles = `opencode/big-pickle`.** The free tier of the `opencode` provider hit its quota (`Rate limit exceeded` / "Free usage exceeded, subscribe to Go" on every `-free` model, including `nemotron-3-ultra-free`) on 2026-09-25. The human declined subscribing to Go, so the project config routes every role to `opencode/big-pickle` (200K context, works on the account).

When the free quota returns (or the human subscribes), restore the 1M set below and restart the app:
- Controller / main session (`build`, `plan`) + final review = `opencode/nemotron-3-ultra-free` (1M).
- Implementers (`general` subagents) = `opencode/mimo-v2.6-flash-free`.
- Exploration (`explore`) + `small_model` = `opencode/ling-3.0-flash-free`.
- Reviewer agent `autocalendar-reviewer` = `opencode/nemotron-3-ultra-free`.
- Other 1M fallbacks: Muse Spark 1.2 `opencode/muse-spark-1.2`, Muse Spark 1.3 `opencode/muse-spark-1.3`, Space Bunny `opencode/space-bunny-free`.

Config lives at `D:\Documents\AIProjects\autoCAlendar\opencode.json`; changes require an app restart to take effect.

## Handoff note (controller change)

Execution is done via superpowers:subagent-driven-development with the plan file as the single source of requirements. The human opens a fresh session (1M-context model) that loads that skill and points it at `docs/superpowers/plans/2026-09-24-autocalendar-mvp.md`; the SDD skill + this ledger are the recovery map. **Nothing has been dispatched yet — Task 1 is the first task to run.** The human does not want the phone connected during development: Tasks 1–12 need no device (unit tests run on the JVM; only `:app:installDebug`/`adb logcat` in Task 13 — on-device verification — need the connected Pixel 10 Pro). Do not attempt Task 13 device steps until the human confirms the phone is connected.

## Task Execution Log

- **Task 1: complete** (commits 6a50897..cb6fe31, review clean)
  - Gradle project scaffold created with all specified files
  - Gradle wrapper bootstrapped (Gradle 8.13, JDK 21)
  - assembleDebug successful
  - Ruling recorded: Compose BOM 2026.09.00 incompatible with AGP 8.13.2 + compileSdk 36; used 2024.10.00 instead
  - Ruling recorded: navigationCompose 2.10.2 and lifecycle 2.11.0 incompatible; used 2.8.4 and 2.8.5 instead

- **Task 2: complete** (commits cb6fe31..7639205, review clean)
  - Domain types created: MeetingDraft, ParseRequest, ParseResult (sealed interface with Success/Failure), ParseFailureReason enum
  - MeetingParser interface created with suspend parse() function
  - Unit test ParseResultTest passes (2 tests)

- **Task 3: complete** (commits 7639205..cdd7eba, review clean)
  - MeetingTextValidator object implemented with validate() returning ParseResult.Failure? for EMPTY_TEXT/TOO_SHORT_TEXT
  - Unit test MeetingTextValidatorTest passes (4 tests)

- **Task 4: complete** (commits cdd7eba..3847b56, review clean)
  - DetectedMeeting schema class with @Generable/@Guide annotations for ML Kit structured output
  - MeetingDraftMapper with ISO date/time parsing and validation (returns null for invalid/missing data)
  - PromptFactory builds prompt with today's date and raw text
  - Unit tests: MeetingDraftMapperTest (5 tests), PromptFactoryTest (1 test) all pass

- **Task 5: complete** (commits 3847b56..7f20331, review clean)
  - EventTimes object with beginMillis/endMillis for timezone-aware epoch millis conversion
  - Unit test EventTimesTest passes (4 tests)

- **Task 6: complete** (commits 7f20331..fefa3bb, review clean)
  - Room entity, DAO, database, and store implementations for history persistence
  - ParsedMeetingEntity, ParsedMeetingDao, AutoCalendarDatabase, ParsedMeetingStore interface, RoomParsedMeetingStore
  - Unit test ParsedMeetingStoreTest passes (2 tests, Robolectric + in-memory Room)

- **Task 7: complete** (commits fefa3bb..124b717, review clean)
  - GeminiNanoParser with ML Kit GenAI Prompt API integration (status check, download, quota retry)
  - PlainDetectedMeetingAdapter for JSON fallback parsing
  - API adapted for beta ML Kit GenAI drift (FeatureStatus as Int, generateContent(String), response text extraction via reflection)
  - Unit test PlainDetectedMeetingAdapterTest passes (3 tests, Robolectric)
  - compileDebugKotlin successful

- **Task 8: complete** (commits 124b717..64e9793, review clean)
  - MainViewModel with text input, loading state, error handling, and in-flight guard
  - MainScreen Composable with text input and parse button
  - MainViewModelTest passes (4 tests) using runTest with injected test scope
  - isLoading guard prevents double-parse

- **Task 9: complete** (commits 64e9793..3bc595f, review clean)
  - CalendarIntentBuilder builds ACTION_INSERT intent with title, begin/end time, location
  - EventToSave data class, CalendarLaunchOutcome sealed interface, CalendarLauncher interface
  - AndroidCalendarLauncher implementation with PackageManager check and ActivityNotFound handling
  - CalendarLauncherTest passes (3 tests, Robolectric)

- **Task 10: complete** (commits 3bc595f..712ecf6, review clean)
  - ConfirmViewModel with draft state, calendar launch, error handling, and history save callback
  - ConfirmScreen Composable with editable fields and create/cancel actions
  - ConfirmViewModelTest passes (5 tests) using runTest with injected test scope
  - Calendar missing handling shows user-facing error
- **Task 11: complete** (commits 1feb36e..ce3aa2e, review clean)
  - HistoryViewModel (store + onSelect, items: StateFlow<List<ParsedMeeting>>, select(item))
  - HistoryScreen Composable matching Task 12 contract exactly (parameter onBack)
  - HistoryViewModelTest passes (2 tests, runTest, no Robolectric), evidence file confirmed on disk (app/build/test-results/.../TEST-com.autocalendar.ui.history.HistoryViewModelTest.xml: tests=2 failures=0 errors=0)
  - Only deviation from plan text: test line uses vm.items.first { it.isNotEmpty() } + import kotlinx.coroutines.flow.first (see the two Task-11 test rulings above)
  - Task 11: minor (deferred): HistoryScreen.kt:32 recomputes ZoneId.systemDefault() per recomposition; hoist to top-level val beside displayFormatter (plan-mandated)
  - Task 11: minor (deferred): no empty-store -> empty items test; plan mandates exactly two tests
  - Task 11: minor (deferred): test pins selected?.id with assertEquals; assertSame would verify the exact tapped instance (plan-mandated)
  - Task 11: Ruling (parked, plan-mandated): HistoryScreen accepts onBack but its body never invokes it; the plan text fixes this at Task 12 (host-level back handling / BackHandler). Carry into the Task 12 dispatch; do not alter Task 11 files.

- **Task 12: Ruling (plan text vs actual code — large reconciliation).** The plan's Task 12 code was written against an architecture that differs from what Tasks 1-11 actually committed; verbatim transcription cannot compile. Enumerated deltas (spec is the binding authority; spec §3/§4 wiring must work end-to-end):
  - Package is com.autocalendar.calendar.* (CalendarLauncher, AndroidCalendarLauncher(context), CalendarIntentBuilder), NOT com.autocalendar.captured.*. AppContainer/factories import the real package.
  - MeetingTextValidator is an object with alidate(rawText): ParseResult.Failure?, not a value of TextValidator; createMainViewModel wraps it as { MeetingTextValidator.validate(it) }.
  - Actual ConfirmViewModel ctor is (launcher: CalendarLauncher, onEventSaved: (EventToSave) -> Unit, scope: CoroutineScope? = null); the draft arrives via onDraftReady(draft, rawText). No store/awText/zone/intentBuilder/onDone params. The factory constructs the VM, wires onEventSaved to save history (spec §4.5: a record is written only when an event was actually created — via container.appScope.launch { container.store.add(...) } then onDone()), then calls onDraftReady(request.draft, request.rawText) to prepopulate fields. Additive VM change: onTitleChange(String) and onLocationChange(String?) setters so the editable Confirm fields (spec §4.3) propagate into the launched/saved event.
  - Actual MainScreen ctor is (viewModel = viewModel(), onDraftReady: (MeetingDraft, String) -> Unit) — no onOpenHistory; plan AppNav passes onOpenHistory. Spec §4.1 mandates a History button on Main. Ruling: MainScreen becomes (viewModel: MainViewModel, onOpenHistory: () -> Unit), drops the vestigial onDraftReady param (nothing uses it; draft ready fires through the MainViewModel ctor callback wired at the Activity), renders a History TextButton, and binds the text field, loading state, and error display to the VM so shared text shows (§3.3) and parse errors surface (§7).
  - Actual ConfirmScreen keeps editable local state and an EMPTY Create-Event onClick. Ruling: bind to VM (title/location/error), wire Create Event -> iewModel.onCreateClick(), show a read-only start-time line (from startMillis) — date/time/duration remain read-only here (PARKED spec deviation §4.3 "all fields editable": a proper datetime editor is separate scope; created events still use draft/recalIl times, which for recalls are the saved post-edit times per plan Task 12 Step 5 note). Cancel keeps nav closure (clear confirmRequest + popBackStack), VM.onCancel() stays as public API but is not wired to the screen.
  - Plan text silent fixes: AppNav confirmRequest param must be MutableStateFlow<ConfirmRequest?> (plan types it StateFlow yet assigns .value = null — compile error otherwise); history recall uses item.toDraft() with awText = item.rawText (saved raw, not the plan's literal "recalled").
  - AppContainer plan text + one addition: al appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default) — the actual ConfirmViewModel has no store dependency, and the wiring must save history asynchronously after a successful calendar launch. ConfirmRequest(draft, rawText), pendingSharedText, confirmRequest as in plan.
  - 
avigation-compose 2.8.4 already declared/wired in Task 1 — no build-file change needed. AndroidManifest SEND filter already present (Task 1).
  - Rationale for reconciliation over re-doing Tasks 8/10: the deltas are exactly what the plan's own Task 12 consumer code expects (onOpenHistory, confirm server flow, store at wiring); the connected app must work per spec, and one Task 12 commit is the smallest change that achieves it. — Cost if wrong: the previously "review clean" Tasks 8/10 files change again (visible in the Task 12 diff), and the parked non-editable date/time/duration surfaces at the final review and to the human.

- **Task 12: complete pending fix round** (commits ce3aa2e..59c5f09, spec compliant per review). Review findings: 0 Critical; 1 Important; 5 Minor. Evidence verified at app/build/test-results/... (11 TEST-*.xml, 35 tests, 0 failures/errors).
- **Task 12: Ruling (navigation hardening — Important finding).** The brief-verbatim AppNav pattern (LaunchedEffect(request) unconditionally navigates whenever request != null; CONFIRM clears the request only in the Cancel lambda; no system-back handling) breaks the back stack under ordinary actions: Activity recreation (rotation) double-pushes CONFIRM, system back leaves the request stale so the next recreation teleports to a stale draft, and the CONFIRM route renders blank if the request is null while the route is on top. The spec (authority) requires working navigation. Ruling: minimal AppNav patch — guard the effect with 
av.currentDestination?.route != Routes.CONFIRM; add BackHandler on the CONFIRM route that mirrors the Cancel lambda (clear request + popBackStack); when the CONFIRM route composes with equest == null, pop back once (LaunchedEffect(Unit)). This keeps every other brief line intact. — Cost if wrong: a slightly imprecise guard on exotic stack states; cheap to re-rule.
- **Task 12: Ruling (§4.5 "actually created" gate).** Reviewer noted the save gate is "calendar intent dispatched without exception", not "user pressed Save in the calendar editor" (ACTION_INSERT cannot observe the final user action without Result API wiring, which is outside MVP). Ruling: the intent-dispatched gate stands; note for the final review as a possible future refinement.
- **Task 12: minor (deferred): MainActivity.handleSharedText runs on every onCreate — stale launch-time EXTRA_TEXT resurrects over a newer onNewIntent share after recreation; consider if (savedInstanceState == null) guard.**
  - Task 12: minor (deferred): screen VMs held by plain emember lose input after back-stack pop to Main / recreation (rememberSaveable or VM hoisting would persist).
  - Task 12: minor (deferred): fire-and-forget store.add has no exception handling; a Room failure crashes the appScope (install a handler or try/catch).
  - Task 12: minor (deferred): finish() on done also closes the app on the history-recall path (friendlier: return to History); share-entry finish() is fine. Brief-mandated; surface to the human at final review.
  - Task 12: minor (deferred): AST reviewer's smart-cast workaround analysis confirmed the two flagged deviations are minimal and semantics-preserving (already documented in task-12-report).

- **Task 12: fix round 1/5** (1 addressed, 0 open — navigation hardening; commits 59c5f09..aad458d)
- **Task 12: complete** (commits ce3aa2e..aad458d, review clean after 1 fix round; re-review: all findings addressed, no new Critical/Important breakage)

- **All headless Tasks 1-12 complete.** Remaining: Task 13 (on-device verification, blocked on the human's Pixel 10 Pro), final whole-branch review, finishing-a-development-branch (merge feat -> main — requires asking the human).

## Final whole-branch review (9940e44..aad458d) — verdict: Request changes

- 2 Critical, 7 Important, ~10 Minor. Architecture/TDD/git hygiene strong. Report: final-review report authored by the reviewer; findings verified by controller:
  - C1 REPRODUCED (javap of gradle-cached genai-prompt-1.0.0-beta4 AAR): generateContent(String) returns GenerateContentResponse; getCandidates(); Candidate.getText(). GeminiNanoParser's reflection 'getMethod("text")' always throws NoSuchMethodException -> every on-device parse returns MODEL_PARSE_FAILED. Also dead line: unused val prompt in runExtraction.
  - I5 CONFIRMED: worktree root .gitignore (HEAD) lacks .worktrees/ and .superpowers/ entries (only the main-checkout copy had them; scaffold commit cb6fe31 rewrote .gitignore).
- **Rulings (fix wave, ONE commit):** fix C1 (candidates->text, beta4), C2 (add manifest <queries> for ACTION_INSERT content), I1 (Events.TITLE="title" key + test), I3 (fill empty history-save test in ConfirmViewModelTest), I4 (friendly userMessage() for ParseFailureReason/LaunchFailureReason + update .name assertions), I5 (.gitignore entries), I6 (rethrow CancellationException before quota handling), M1 (savedInstanceState==null guard + setIntent in onNewIntent), M2 (ConfirmRequest.finishOnDone: recall path returns to History instead of finish()), M3 (try/catch around fire-and-forget store.add), M4 (named status constants), M5 (dead code: unused typealias/imports, ResolveInfo, Activity branch), M7 (hoist ZoneId via remember), M9 (harden PlainDetectedMeetingAdapter fences/prose + tests), M10 (blank-title guard).
- **Rulings (parked):** I2 structured-output path stays unimplemented (plain-JSON is the active fallback; S.O. is alpha and device-gated; cost if wrong: partial spec 4.2 compliance; revisit post-MVP); I7 editable date/time per spec 4.3 — HUMAN feature decision deferred to User checkpoint; M6 commit-message themes frozen (history not rewritten); M8 lint icon/ktx polish needs an icon asset — deferred; M7 onBack param is plan-mandated, kept.
- Next: one fix-wave dispatch (implementer), then scoped re-review of the fix diff, then Task 13 (device) + human checkpoint, then finishing-a-development-branch.

## Fix wave 1 (aad458d..05c835c) — verified

- One commit  5c835c "fix: address final review findings" (17 files). C1..M10 all addressed. Evidence: compile BUILD SUCCESSFUL, tests 38/0/0 (11 classes), assembleDebug BUILD SUCCESSFUL, tree clean. Report: final-review-fix-report.md.
- Re-review (scoped aad458d..05c835c): all 15 findings ADDRESSED; 0 Critical/Important; 3 new minors (adapter brace-scan prose {, invocation-count in I3 test, M10 not tested).

## Fix wave 2 (05c835c..1f02b66) — verified clean

- One commit 1f02b66 "fix: close re-review minors" (3 files). PlainDetectedMeetingAdapter now scans balanced {..} candidates skipping string-literal braces; ConfirmViewModelTest gains invocation-count assert + blank-title test.
- Recheck: all 3 items closed, no new breakage; tests 40/0/0 (11 classes). Ready for on-device Task 13.
- **Whole-branch review: COMPLETE/CLEAN. Branch HEAD = 1f02b66. Remaining: Task 13 (device, needs the human's Pixel 10 Pro), human checkpoint (I7 4.3 editable fields decision, I2 structured-output deferral sign-off, merge approval), finishing-a-development-branch.**

## Human checkpoint (2026-09-25) — decisions

- **Ruling: I7 ACCEPTED for MVP (human approval).** Date/time/duration stay read-only in Confirm (editable title/location only); correction is fully available inside the calendar editor after ACTION_INSERT. Editable field editors are out of MVP scope. Cost if wrong: users must edit date/time in the calendar app (extra tap), spec 4.3 partially unmet; revisit post-MVP.
- **Ruling: merge deferred until after Task 13 (human choice).**
- Task 13: human is connecting the Pixel 10 Pro now.
- I2 structured-output decision: PENDING — human asked for a clearer explanation (see follow-up question).

## Human checkpoint (2026-09-25) - decisions

- **Ruling: I7 ACCEPTED for MVP (human approval).** Date/time/duration stay read-only in Confirm (editable title/location only); correction is fully available inside the calendar editor after ACTION_INSERT. Editable field editors are out of MVP scope. Cost if wrong: users must edit date/time in the calendar app (extra tap), spec 4.3 partially unmet; revisit post-MVP.
- **Ruling: merge deferred until after Task 13 (human choice).**
- Task 13: human is connecting the Pixel 10 Pro now.
- I2 structured-output decision: PENDING - human asked for a clearer explanation (see follow-up question).

- **Ruling: I2 DEFERRED to post-MVP (human approval).** Structured Output stays unimplemented; the JSON fallback path is the active parse path (self-sufficient for the MVP). Cost if wrong: partial spec 4.2 compliance; structured output may be more robust on capable devices; revisit post-MVP.
- Human decisions complete; Task 13 may proceed once the Pixel 10 Pro is visible via adb.

## Task 13: complete (on-device, commits 05dc5a2..3094e68) — all steps pass

Device: Pixel 10 Pro (serial 61060DLCH005PV), 2026-09-25. Evidence: docs/verification-notes.md (committed), UI dumps in session, calendar provider row _id=769.

- Step 1 (AICore): com.google.android.aicore present; nano-v3 model download required VPN pause (Riot VPN llc.itdev.incy) before it started flowing; re-enabled after (always-on was not set). Model verified via successful inference.
- Step 2 (install): :app:installDebug BUILD SUCCESSFUL.
- Step 3 (happy path): SEND text -> Main shows text -> Extract -> Parsing -> Confirm (Meeting at Starbucks, 24 Sept 2026 15:00) -> Create Event -> Google Calendar editor prefilled -> Save -> row persisted (title/dtstart/dtend/eventLocation verified via content query). App activity finishes; History shows the record; tap reopens Confirm. PASS. FINDING+FIX: <queries> scheme-only hid the calendar -> added mimeType vnd.android.cursor.dir/event entry (commit b23cde2).
- Step 4.1 (next Friday): model returned fixed Wednesday 30 Sept 2026 for RU and EN alike despite the "today" anchor. FINDING+FIX: NextWeekdayDateCorrector (deterministic ISO-week+1, RU+EN, prefix/declension-tolerant) wired into GeminiNanoParser (commit 062f298). After fix RU and EN both -> 02 Oct 2026 19:00. Adjacent model strengths verified: завтра/сегодня/absolute/plain weekday all correct.
- Step 4.2 (multi-message): two-speaker dialog -> single meeting "Meeting about project / 29 Sept 2026 11:00" (Tuesday) PASS.
- Step 5 (failure paths): "ok" -> TOO_SHORT (no model call) PASS; date without time -> MISSING_DATE_OR_TIME PASS; airplane mode on -> parse succeeded fully offline PASS.
- Step 6 (fix round): 4 follow-up commits (manifest visibility b23cde2; relative-date corrector + prompt 062f298; verification notes 4fff722; AGENTS.md gotchas 3094e68). Final suite: 47 tests / 0 failures / 12 classes. Device state fully restored (airplane off, stay_on_while_plugged_in 0, screen_off_timeout 60000, VPN enabled=1).

- Ruling (MVP): the beta AICore model cannot reliably resolve "next <weekday>"; MVP defends with a deterministic corrector rather than trusting the model. Cost if wrong: a narrow RU/EN language pattern handled in-app; other relative phrases (послезавтра, через неделю) stay model-dependent.
- Whole plan complete: Tasks 1-13 done, final review clean. Next: finishing-a-development-branch (merge feat -> main; human approved merge after Task 13; expect a trivial AGENTS.md/docs conflict at the merge as both branches carry doc commits).
