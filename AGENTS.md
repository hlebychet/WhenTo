# AGENTS.md

## Repo layout

- `main` holds the whole project: the Android app under `app/`, build files at the root, and docs under `docs/` (`docs/superpowers/specs/…-mvp-design.md`, `docs/superpowers/plans/…-mvp.md`, `docs/verification-notes.md`, `docs/superpowers/sdd/…` — the SDD execution archive).
- The `feat/autocalendar-mvp` branch was merged into `main` on 2026-09-25 and its worktree removed. Future feature work uses fresh worktrees via superpowers:using-git-worktrees — never commit directly on `main`.
- `.worktrees/`, `.superpowers/`, `local.properties` are git-ignored.

## Roles and decisions

- The human decides **features and requirements** (what the app must do). The agent decides **everything about the process** — build, tooling, execution, rulings — and should not stop to ask about process choices.
- An agent stops to ask only for: an irreversible/destructive action, a security-sensitive action, a side effect outside the repo (merge/push to a shared branch, touching the human's devices), or a plan so broken that every path forward is a guess.
- When plan text conflicts with the spec, the **spec is the binding authority**; record each judgment as a `Ruling: <what — why — what it costs if wrong>` line in the SDD ledger and keep going.

## Git workflow

- `main` = stable. All feature work happens on a feature branch in a worktree (see Repo layout), integrated back via superpowers:finishing-a-development-branch.
- **No git remote is configured** — don't push, and don't create a remote without asking.
- One commit per plan task, using the exact `git add`+`git commit` command the plan prescribes for that step. Never batch multiple tasks into one commit; never skip a task's commit.
- Keep `AGENTS.md` content identical on both branches (commit on one, fast-forward the other).

## Build environment (Windows, pwsh)

- Set `JAVA_HOME` before any gradle command — system JDK 24/25 is too new for Gradle 8.13; use Android Studio's JBR (JDK 21):
  `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`
- Android SDK at `%LOCALAPPDATA%\Android\Sdk`, wired via git-ignored `local.properties` (`sdk.dir`).
- The SDK has **no `cmdline-tools`/`sdkmanager`** — don't try to install SDK packages from the CLI; use Android Studio's UI if packages are ever missing.
- No system gradle — always use the wrapper from the worktree root: `.\gradlew.bat …`
- Commands (from the worktree root):
  - Debug APK: `.\gradlew.bat :app:assembleDebug`
  - Compile only: `.\gradlew.bat :app:compileDebugKotlin`
  - All unit tests: `.\gradlew.bat :app:testDebugUnitTest`
  - One test class: `.\gradlew.bat :app:testDebugUnitTest --tests "com.autocalendar.<pkg>.<TestClass>"`
  - Install to a connected device: `.\gradlew.bat :app:installDebug`
  - Live device logs: `adb logcat`

## The plan workflow

- Authoritative docs: spec (architecture, requirements) and plan (13 tasks, TDD, full code per step). The plan's code blocks are to be transcribed verbatim; versions are pinned in `gradle/libs.versions.toml` — do not bump them.
- Execution status is tracked in the SDD ledger `.worktrees/autocalendar-mvp/.superpowers/sdd/2026-09-24-autocalendar-mvp/progress.md`. Read it before starting any work; task briefs/reports live next to it. Tasks have `Task <N>: complete` lines when done.
- Follow the tasks in order: failing test first, then implementation, then the commit the plan prescribes. Do not batch tasks or skip tests.
- Task 1 bootstraps the Gradle scaffold; Task 13 is on-device verification and needs a connected Pixel 10 Pro.

## Device testing

- Gemini Nano runs via ML Kit GenAI Prompt API on AICore. Only Pixel 10 Pro is a valid test target; Pixel 7 Pro lacks the model for this API. The model must be downloaded (AICore experimental enrollment) before Task 13.
- The phone stays with the human during development — don't run device steps (install/logcat/verify) until Task 13 and the human confirms the Pixel 10 Pro is connected. Tasks 1–12 run headless (JVM unit tests only).
- `genai-prompt:1.0.0-beta4` / `genai-schema-compiler:1.0.0-alpha1` are beta/alpha — Task 7 Step 4 allows adapting call sites to the current API if signatures drift. Keep the ProGuard `-keep` rule for `com.autocalendar.parser.DetectedMeeting` (structured-output schema class).
- To verify the ML Kit beta API surface headlessly, `javap` the real AAR from the gradle cache: `Get-ChildItem ~\.gradle\caches -Recurse -Filter "genai-prompt-1.0.0-beta4.aar"`, `Expand-Archive`, then `javap -cp classes.jar <class>`. In beta4 `generateContent(prompt: String)` returns `GenerateContentResponse` (not a String) with `getCandidates()`; text comes from `Candidate.getText()` — any reflection-based response extraction will silently fail at runtime.
- The nano-v3 model's `download()` can sit idle while AICore waits: the download only started flowing here after the phone's corporate VPN (Riot VPN, `llc.itdev.incy`) was paused via `pm disable-user --user 0 <pkg>` (restore with `pm enable <pkg>`; always-on VPN was not set on this device). The model also resolves relative dates unreliably (`next Friday` came back as a fixed wrong Wednesday in RU and EN) — MVP defends with `NextWeekdayDateCorrector`, a deterministic ISO-week+1 pass.
- Kotlin `Regex` `\b`/`\w` are ASCII-only — they do not treat Cyrillic as word characters, so markers like "следующ…" must be matched with `[а-яё]+` token scans, not `\b`+`\w`. Mirror `NextWeekdayDateCorrector.russianWeekdays()`.

## Keep this file current

- When you discover something during work that a future session would have to rediscover the hard way — a machine/environment quirk, a toolchain gotcha that cost you time, a command that must run in a specific order, a workflow step — append it to this file before you finish. Not trivia: only global, reusable facts another session will actually benefit from. One-off workarounds and task-specific details go in the plan/ledger, not here.
- This file lives on both `main` and the `feat/autocalendar-mvp` worktree with identical content. After editing it, keep the two in sync (commit on one branch, then fast-forward/merge the other) so the next session sees it regardless of which checkout it opens.

## Conventions

- Code, docs, and commit messages: English. Chat replies to the human: Russian.
- Package root `com.autocalendar`; `minSdk 26`, `compileSdk/targetSdk 36`, Java/Kotlin target 17.
- Architecture: single Activity + Compose, MVVM over small interfaces (`MeetingParser`, `ParsedMeetingStore`, `CalendarLauncher`); pure-Kotlin logic is unit-tested without Android.
- No emojis anywhere: code, UI strings, comments, docs, and commit messages may only use ASCII and typographic punctuation (e.g. arrows →, en dashes –, curly quotes).