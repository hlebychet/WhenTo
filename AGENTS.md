# AGENTS.md

## Repo layout

- `main` checkout holds only docs (`docs/superpowers/specs/…-mvp-design.md`, `docs/superpowers/plans/…-mvp.md`). The app code is **not** on `main`.
- All implementation lives in the worktree `.worktrees/autocalendar-mvp` (branch `feat/autocalendar-mvp`). Work there, never on `main`.
- `.worktrees/`, `.superpowers/`, `local.properties` are git-ignored.

## Build environment (Windows, pwsh)

- Set `JAVA_HOME` before any gradle command — system JDK 24/25 is too new for Gradle 8.13; use Android Studio's JBR (JDK 21):
  `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`
- Android SDK at `%LOCALAPPDATA%\Android\Sdk`, wired via git-ignored `local.properties` (`sdk.dir`).
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
- `genai-prompt:1.0.0-beta4` / `genai-schema-compiler:1.0.0-alpha1` are beta/alpha — Task 7 Step 4 allows adapting call sites to the current API if signatures drift. Keep the ProGuard `-keep` rule for `com.autocalendar.parser.DetectedMeeting` (structured-output schema class).

## Keep this file current

- When you discover something during work that a future session would have to rediscover the hard way — a machine/environment quirk, a toolchain gotcha that cost you time, a command that must run in a specific order, a workflow step — append it to this file before you finish. Not trivia: only global, reusable facts another session will actually benefit from. One-off workarounds and task-specific details go in the plan/ledger, not here.
- This file lives on both `main` and the `feat/autocalendar-mvp` worktree with identical content. After editing it, keep the two in sync (commit on one branch, then fast-forward/merge the other) so the next session sees it regardless of which checkout it opens.

## Conventions

- Code, docs, and commit messages: English. Chat replies to the human: Russian.
- Package root `com.autocalendar`; `minSdk 26`, `compileSdk/targetSdk 36`, Java/Kotlin target 17.
- Architecture: single Activity + Compose, MVVM over small interfaces (`MeetingParser`, `ParsedMeetingStore`, `CalendarLauncher`); pure-Kotlin logic is unit-tested without Android.