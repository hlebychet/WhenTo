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

## Model roles (configured in repo-root opencode.json)

The human switched the project to role-based models (provider `opencode`, IDs verified from the live registry `models.opencode.ai/api.json`):

- Controller / main session (`build`, `plan`) + final whole-branch review = `opencode/nemotron-3-ultra-free` (1M context).
- Implementers (Task-dispatched `general` subagents) = `opencode/mimo-v2.6-flash-free`.
- Exploration (`explore`) + titles/`small_model` = `opencode/ling-3.0-flash-free`.
- Dedicated reviewer subagent `autocalendar-reviewer` = `opencode/nemotron-3-ultra-free`.
- Fallbacks if a model is unavailable: Muse Spark 1.2 `opencode/muse-spark-1.2`, Muse Spark 1.3 `opencode/muse-spark-1.3`, Space Bunny `opencode/space-bunny-free` (all 1M); Ling 3.0 Flash Free `opencode/ling-3.0-flash-free`.

Config lives at `D:\Documents\AIProjects\autoCAlendar\opencode.json`; changes require an app restart to take effect.

## Handoff note (controller change)

Execution is done via superpowers:subagent-driven-development with the plan file as the single source of requirements. The human opens a fresh session (1M-context model) that loads that skill and points it at `docs/superpowers/plans/2026-09-24-autocalendar-mvp.md`; the SDD skill + this ledger are the recovery map. **Nothing has been dispatched yet — Task 1 is the first task to run.** The human does not want the phone connected during development: Tasks 1–12 need no device (unit tests run on the JVM; only `:app:installDebug`/`adb logcat` in Task 13 — on-device verification — need the connected Pixel 10 Pro). Do not attempt Task 13 device steps until the human confirms the phone is connected.