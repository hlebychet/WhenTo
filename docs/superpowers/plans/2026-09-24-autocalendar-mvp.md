# AutoCalendar MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android app that extracts a meeting (title, date, time, duration, location) from shared messenger text using on-device Gemini Nano, lets the user confirm/edit the result, creates a calendar event, and keeps a local history.

**Architecture:** Single-Activity, Jetpack Compose UI, MVVM with ViewModels that consume small interfaces (`MeetingParser`, `ParsedMeetingStore`, `CalendarLauncher`). The parser implementation calls ML Kit GenAI Prompt API (Gemini Nano via AICore) with Structured Output where available, falling back to a plain-prompt JSON parse. Clean pure-Kotlin logic (validation, ISO→datetime mapping, event-time math) is unit-tested without Android; the Gemini Nano path is verified on the target device.

**Tech Stack:** Kotlin 2.3.21, AGP 8.13.2, Compose BOM 2026.09.00, Material 3, Navigation Compose 2.10.2, Room 2.8.5 (KSP), ML Kit `genai-prompt:1.0.0-beta4` + `genai-schema-compiler:1.0.0-alpha1` (KSP), coroutines. Tests: JUnit 4, kotlinx-coroutines-test, Robolectric 4.15.1.

**Spec:** `docs/superpowers/specs/2026-09-24-autocalendar-mvp-design.md`

## Global Constraints

- Package root: `com.autocalendar` everywhere.
- One Activity (`MainActivity`, `ComponentActivity`, Compose-only, `android:exported="true"`).
- `minSdk 26`, `compileSdk 36`, `targetSdk 36`, Java/Kotlin target 17.
- Dependency versions (pinned in `gradle/libs.versions.toml`, do not guess):
  - `com.google.mlkit:genai-prompt:1.0.0-beta4`
  - `com.google.mlkit:genai-schema-compiler:1.0.0-alpha1` via `ksp(...)`
  - `androidx.room:room-runtime:2.8.5` + `room-compiler:2.8.5` (ksp)
  - Compose BOM `2026.09.00`; `navigation-compose:2.10.2`; `lifecycle-viewmodel-compose:2.11.0`; `activity-compose:1.13.0`
  - Kotlin `2.3.21`, KSP `2.3.12` (req: KSP >= 2.3.6), AGP `8.13.2`, Gradle wrapper `8.13`
  - Test: `junit:4.13.2`, `kotlinx-coroutines-test:1.10.2`, `org.robolectric:robolectric:4.15.1`, `androidx.test:core:1.6.1`
- The `@Generable` schema data class must be kept out of ProGuard; add a keep rule for it.
- No cloud calls. No regex-based meeting parsing (living-language text; the model does the understanding).
- UI strings in English for MVP.
- The app must never create a calendar event without the user pressing "Create event" (spec §3.6, §7).

## Review Focus

Failure modes the spec implies that no task's tests exercise them directly but a real user will hit; each line is pinned to a test in the owning task:

1. **Shared text contains no meeting at all** (e.g. "ok") → app must show a clear error, not fail silently or crash. → Task 3 validation tests.
2. **Messengers sometimes share empty/HTML-extracted text** → blank or whitespace-only share must be rejected before calling the model. → Task 3 blank-input test.
3. **Model returns a date but no time** → without a time the event cannot be placed; treat as failure with a message. → Task 4 mapper test.
4. **User re-shares while a parse is in flight** → ignore the new parse, show progress, don't double-launch. → Task 8 ViewModel `isLoading` guard test.
5. **Calendar app missing** (emulator/fresh device) → `ActivityNotFoundException` must surface as a readable message, not a crash, and the history record should still be saved. → Task 10 launcher test + ConfirmViewModel test.

---

### Task 1: Gradle project scaffold

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/autocalendar/AutoCalendarApp.kt`
- Create: `app/src/main/java/com/autocalendar/MainActivity.kt`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `.gitignore`

**Interfaces:**
- Produces: buildable empty app; `AndroidManifest.xml` with the SEND intent-filter; `com.autocalendar.MainActivity` that renders a placeholder.

- [ ] **Step 1: Create the version catalog** `gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.13.2"
kotlin = "2.3.21"
ksp = "2.3.12"
coreKtx = "1.17.0"
composeBom = "2026.09.00"
activityCompose = "1.13.0"
lifecycle = "2.11.0"
navigationCompose = "2.10.2"
room = "2.8.5"
coroutinesTest = "1.10.2"
mlkitGenaiPrompt = "1.0.0-beta4"
genaiSchemaCompiler = "1.0.0-alpha1"
junit = "4.13.2"
robolectric = "4.15.1"
androidxTestCore = "1.6.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
mlkit-genai-prompt = { group = "com.google.mlkit", name = "genai-prompt", version.ref = "mlkitGenaiPrompt" }
genai-schema-compiler = { group = "com.google.mlkit", name = "genai-schema-compiler", version.ref = "genaiSchemaCompiler" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
androidx-test-core = { group = "androidx.test", name = "core", version.ref = "androidxTestCore" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 2: Create `settings.gradle.kts`:**

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AutoCalendar"
include(":app")
```

- [ ] **Step 3: Create root `build.gradle.kts`:**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 4: Create `gradle.properties`:**

```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
```

- [ ] **Step 5: Create `gradle/wrapper/gradle-wrapper.properties`:**

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

Run `gradle wrapper --gradle-version 8.13` from the project root if the wrapper scripts are missing (or generate the wrapper from Android Studio).

- [ ] **Step 6: Create `app/build.gradle.kts`:**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.autocalendar"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.autocalendar"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.mlkit.genai.prompt)
    ksp(libs.genai.schema.compiler)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
```

- [ ] **Step 7: Create `app/proguard-rules.pro`:**

```proguard
-keep class com.autocalendar.parser.DetectedMeeting { *; }
```

- [ ] **Step 8: Create `app/src/main/AndroidManifest.xml`:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name=".AutoCalendarApp"
        android:label="@string/app_name"
        android:theme="@style/Theme.AutoCalendar"
        android:allowBackup="true"
        android:supportsRtl="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.SEND" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="text/plain" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

- [ ] **Step 9: Create `app/src/main/res/values/themes.xml`:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.AutoCalendar" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 10: Create `app/src/main/res/values/strings.xml`:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">AutoCalendar</string>
</resources>
```

- [ ] **Step 11: Create `.gitignore`:**

```gitignore
.gradle/
build/
local.properties
.idea/
*.iml
captures/
.externalNativeBuild/
.cxx/
```

- [ ] **Step 12: Create `app/src/main/java/com/autocalendar/AutoCalendarApp.kt`:**

```kotlin
package com.autocalendar

import android.app.Application

class AutoCalendarApp : Application()
```

- [ ] **Step 13: Create `app/src/main/java/com/autocalendar/MainActivity.kt`:**

```kotlin
package com.autocalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Text("AutoCalendar")
            }
        }
    }
}
```

- [ ] **Step 14: Verify build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Fix any version/DSL incompatibility you hit here before continuing; this is the whole toolchain for the rest of the plan.

- [ ] **Step 15: Commit**

```bash
git init
git add .
git commit -m "chore: scaffold Android Gradle project"
```

---

### Task 2: Domain types and the parser interface

**Files:**
- Create: `app/src/main/java/com/autocalendar/domain/MeetingDraft.kt`
- Create: `app/src/main/java/com/autocalendar/domain/ParseRequest.kt`
- Create: `app/src/main/java/com/autocalendar/domain/ParseResult.kt`
- Create: `app/src/main/java/com/autocalendar/parser/MeetingParser.kt`
- Create: `app/src/test/java/com/autocalendar/domain/ParseResultTest.kt`

**Interfaces:**
- Produces (later tasks consume these exact shapes):
  - `data class MeetingDraft(val title: String, val startDateTime: java.time.LocalDateTime, val durationMinutes: Int?, val location: String?)`
  - `data class ParseRequest(val rawText: String, val today: java.time.LocalDate)`
  - `sealed interface ParseResult { data class Success(val meeting: MeetingDraft); data class Failure(val reason: ParseFailureReason) }`
  - `enum class ParseFailureReason { EMPTY_TEXT, TOO_SHORT_TEXT, NANO_UNAVAILABLE, MISSING_DATE_OR_TIME, MODEL_PARSE_FAILED, QUOTA_EXCEEDED, SYSTEM_CALENDAR_MISSING }`
  - `interface MeetingParser { suspend fun parse(request: ParseRequest): ParseResult }`

- [ ] **Step 1: Write the failing test** `app/src/test/java/com/autocalendar/domain/ParseResultTest.kt`:

```kotlin
package com.autocalendar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class ParseResultTest {

    @Test
    fun `success carries the meeting draft through`() {
        val meeting = MeetingDraft(
            title = "Discuss mockup",
            startDateTime = LocalDateTime.of(2026, 9, 25, 15, 0),
            durationMinutes = 30,
            location = null,
        )
        val result: ParseResult = ParseResult.Success(meeting)
        val success = result as? ParseResult.Success ?: throw AssertionError("Expected Success")
        assertEquals("Discuss mockup", success.meeting.title)
        assertNull(success.meeting.location)
    }

    @Test
    fun `failure carries the reason`() {
        val result: ParseResult = ParseResult.Failure(ParseFailureReason.TOO_SHORT_TEXT)
        val failure = result as? ParseResult.Failure ?: throw AssertionError("Expected Failure")
        assertEquals(ParseFailureReason.TOO_SHORT_TEXT, failure.reason)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.domain.ParseResultTest"`
Expected: FAIL, compilation error — types don't exist yet.

- [ ] **Step 3: Implement the domain types**

`app/src/main/java/com/autocalendar/domain/MeetingDraft.kt`:

```kotlin
package com.autocalendar.domain

import java.time.LocalDateTime

data class MeetingDraft(
    val title: String,
    val startDateTime: LocalDateTime,
    val durationMinutes: Int?,
    val location: String?,
)
```

`app/src/main/java/com/autocalendar/domain/ParseRequest.kt`:

```kotlin
package com.autocalendar.domain

import java.time.LocalDate

data class ParseRequest(
    val rawText: String,
    val today: LocalDate,
)
```

`app/src/main/java/com/autocalendar/domain/ParseResult.kt`:

```kotlin
package com.autocalendar.domain

sealed interface ParseResult {
    data class Success(val meeting: MeetingDraft) : ParseResult
    data class Failure(val reason: ParseFailureReason) : ParseResult
}

enum class ParseFailureReason {
    EMPTY_TEXT,
    TOO_SHORT_TEXT,
    NANO_UNAVAILABLE,
    MISSING_DATE_OR_TIME,
    MODEL_PARSE_FAILED,
    QUOTA_EXCEEDED,
    SYSTEM_CALENDAR_MISSING,
}
```

`app/src/main/java/com/autocalendar/parser/MeetingParser.kt`:

```kotlin
package com.autocalendar.parser

import com.autocalendar.domain.ParseRequest
import com.autocalendar.domain.ParseResult

interface MeetingParser {
    suspend fun parse(request: ParseRequest): ParseResult
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.domain.ParseResultTest"`
Expected: PASS, 2 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/autocalendar/domain app/src/main/java/com/autocalendar/parser app/src/test/java/com/autocalendar/domain app/src/main/java/com/autocalendar/parser/*.kt
git commit -m "feat: domain types and MeetingParser interface"
```

---

### Task 3: Input validation for shared text

**Files:**
- Create: `app/src/main/java/com/autocalendar/parser/MeetingTextValidator.kt`
- Create: `app/src/test/java/com/autocalendar/parser/MeetingTextValidatorTest.kt`

**Interfaces:**
- Consumes: `ParseResult`, `ParseFailureReason` (Task 2).
- Produces: `object MeetingTextValidator { fun validate(rawText: String): ParseResult.Failure? }` — returns `null` when OK, else a `Failure` with `EMPTY_TEXT` / `TOO_SHORT_TEXT`.

- [ ] **Step 1: Write the failing test** `app/src/test/java/com/autocalendar/parser/MeetingTextValidatorTest.kt` — pins Review Focus 1 and 2:

```kotlin
package com.autocalendar.parser

import com.autocalendar.domain.ParseFailureReason
import com.autocalendar.domain.ParseResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeetingTextValidatorTest {

    @Test
    fun `blank input is rejected with EMPTY_TEXT`() {
        val failure = MeetingTextValidator.validate("   \n\t  ")
        val reason = (failure as? ParseResult.Failure)?.reason
        assertEquals(ParseFailureReason.EMPTY_TEXT, reason)
    }

    @Test
    fun `empty share string is rejected with EMPTY_TEXT`() {
        val failure = MeetingTextValidator.validate("")
        val reason = (failure as? ParseResult.Failure)?.reason
        assertEquals(ParseFailureReason.EMPTY_TEXT, reason)
    }

    @Test
    fun `no-meeting text like ok is rejected as TOO_SHORT`() {
        val failure = MeetingTextValidator.validate("ok")
        val reason = (failure as? ParseResult.Failure)?.reason
        assertEquals(ParseFailureReason.TOO_SHORT_TEXT, reason)
    }

    @Test
    fun `a plausible meeting message passes`() {
        assertNull(MeetingTextValidator.validate("Let's meet on Thursday at 3pm to discuss the mockup."))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.parser.MeetingTextValidatorTest"`
Expected: FAIL, `MeetingTextValidator` not defined.

- [ ] **Step 3: Implement the validator** `app/src/main/java/com/autocalendar/parser/MeetingTextValidator.kt`:

```kotlin
package com.autocalendar.parser

import com.autocalendar.domain.ParseFailureReason
import com.autocalendar.domain.ParseResult

object MeetingTextValidator {

    private const val MIN_MEANINGFUL_LENGTH = 12

    fun validate(rawText: String): ParseResult.Failure? {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            return ParseResult.Failure(ParseFailureReason.EMPTY_TEXT)
        }
        if (trimmed.length < MIN_MEANINGFUL_LENGTH) {
            return ParseResult.Failure(ParseFailureReason.TOO_SHORT_TEXT)
        }
        return null
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.parser.MeetingTextValidatorTest"`
Expected: PASS, 4 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/autocalendar/parser/MeetingTextValidator.kt app/src/test/java/com/autocalendar/parser/MeetingTextValidatorTest.kt
git commit -m "feat: validate shared text before invoking the model"
```

---

### Task 4: Gemini Nano schema and ISO mapping

**Files:**
- Create: `app/src/main/java/com/autocalendar/parser/DetectedMeeting.kt`
- Create: `app/src/main/java/com/autocalendar/parser/MeetingDraftMapper.kt`
- Create: `app/src/test/java/com/autocalendar/parser/MeetingDraftMapperTest.kt`

**Interfaces:**
- Consumes: `MeetingDraft`, `ParseFailureReason`, `ParseResult` (Task 2).
- Produces:
  - `@Generable data class DetectedMeeting(@Guide val title: String, @Guide val date: String, @Guide val time: String, @Guide val durationMinutes: Int?, @Guide val location: String?)` — annotations from `com.google.mlkit.genai.schema.annotations`; date `YYYY-MM-DD`, time `HH:mm`, nulls allowed for duration/location.
  - `class MeetingDraftMapper { fun fromDetected(detected: DetectedMeeting): MeetingDraft? }` — returns `null` when title, date or time is blank/unparseable; `date` fail → null (surfacing as `MISSING_DATE_OR_TIME` at the caller).
  - `object PromptFactory { fun build(rawText: String, today: LocalDate): String }`

- [ ] **Step 1: Write the failing test** `app/src/test/java/com/autocalendar/parser/MeetingDraftMapperTest.kt` — pins Review Focus 3:

```kotlin
package com.autocalendar.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class MeetingDraftMapperTest {

    private val mapper = MeetingDraftMapper()

    @Test
    fun `maps valid detected meeting`() {
        val detected = DetectedMeeting(
            title = "Discuss mockup",
            date = "2026-09-25",
            time = "15:00",
            durationMinutes = 30,
            location = "Starbucks",
        )

        val draft = mapper.fromDetected(detected) ?: throw AssertionError("Expected draft")

        assertEquals("Discuss mockup", draft.title)
        assertEquals(LocalDateTime.of(2026, 9, 25, 15, 0), draft.startDateTime)
        assertEquals(30, draft.durationMinutes)
        assertEquals("Starbucks", draft.location)
    }

    @Test
    fun `maps missing location and duration to null`() {
        val detected = DetectedMeeting(
            title = "Call",
            date = "2026-09-25",
            time = "19:00",
            durationMinutes = null,
            location = null,
        )

        val draft = mapper.fromDetected(detected) ?: throw AssertionError("Expected draft")

        assertNull(draft.durationMinutes)
        assertNull(draft.location)
    }

    @Test
    fun `date without time returns null`() {
        val detected = DetectedMeeting(
            title = "Call",
            date = "2026-09-25",
            time = "",
            durationMinutes = null,
            location = null,
        )

        assertNull(mapper.fromDetected(detected))
    }

    @Test
    fun `blank title returns null`() {
        val detected = DetectedMeeting(
            title = "   ",
            date = "2026-09-25",
            time = "19:00",
            durationMinutes = null,
            location = null,
        )

        assertNull(mapper.fromDetected(detected))
    }

    @Test
    fun `garbage date returns null`() {
        val detected = DetectedMeeting(
            title = "Call",
            date = "not-a-date",
            time = "19:00",
            durationMinutes = null,
            location = null,
        )

        assertNull(mapper.fromDetected(detected))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.parser.MeetingDraftMapperTest"`
Expected: FAIL — `DetectedMeeting` / `MeetingDraftMapper` undefined.

- [ ] **Step 3: Implement the schema class** `app/src/main/java/com/autocalendar/parser/DetectedMeeting.kt`:

```kotlin
package com.autocalendar.parser

import com.google.mlkit.genai.schema.annotations.Generable
import com.google.mlkit.genai.schema.annotations.Guide

@Generable("A meeting extracted from a messenger message")
data class DetectedMeeting(
    @Guide(description = "Short descriptive meeting title")
    val title: String,

    @Guide(description = "Meeting date in YYYY-MM-DD format")
    val date: String,

    @Guide(description = "Meeting start time in 24h HH:mm format")
    val time: String,

    @Guide(description = "Meeting duration in minutes, or null if not mentioned",
        minimum = 1, maximum = 1440)
    val durationMinutes: Int?,

    @Guide(description = "Meeting location, or null if not mentioned")
    val location: String?,
)
```

- [ ] **Step 4: Implement the mapper** `app/src/main/java/com/autocalendar/parser/MeetingDraftMapper.kt`:

```kotlin
package com.autocalendar.parser

import com.autocalendar.domain.MeetingDraft
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class MeetingDraftMapper {

    fun fromDetected(detected: DetectedMeeting): MeetingDraft? {
        val title = detected.title.trim()
        val date = LocalDate.parseOrNull(detected.date) ?: return null
        val time = LocalTimeText.parseOrNull(detected.time) ?: return null
        if (title.isEmpty()) return null

        val startDateTime = LocalDateTime.of(date, time)
        val duration = detected.durationMinutes?.takeIf { it > 0 }

        return MeetingDraft(
            title = title,
            startDateTime = startDateTime,
            durationMinutes = duration,
            location = detected.location?.trim()?.takeIf { it.isNotEmpty() },
        )
    }
}

private fun LocalDate.parseOrNull(text: String): LocalDate? =
    try {
        LocalDate.parse(text.trim())
    } catch (e: DateTimeParseException) {
        null
    }

private object LocalTimeText {
    private val regex = Regex("^([01]?\\d|2[0-3]):([0-5]\\d)$")

    fun parseOrNull(text: String): java.time.LocalTime? {
        val m = regex.matchEntire(text.trim()) ?: return null
        val hour = m.groupValues[1].toInt()
        val minute = m.groupValues[2].toInt()
        return java.time.LocalTime.of(hour, minute)
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.parser.MeetingDraftMapperTest"`
Expected: PASS, 5 tests.

- [ ] **Step 6: Implement the prompt factory** `app/src/main/java/com/autocalendar/parser/PromptFactory.kt`:

```kotlin
package com.autocalendar.parser

import java.time.LocalDate

object PromptFactory {

    fun build(rawText: String, today: LocalDate): String =
        """
        Today's date is ${today} (YYYY-MM-DD).

        A user pasted one or more messenger messages that arrange a meeting.
        Extract the meeting described in this text and base any relative dates
        ("tomorrow", "on Thursday", "next Friday") on today's date above.

        The title must be short and descriptive. Output the schema fields only —
        leave any field empty or null when the text does not say it.

        Messages:
        $rawText
        """.trimIndent()
}
```

- [ ] **Step 7: Add a test for the prompt factory** `app/src/test/java/com/autocalendar/parser/PromptFactoryTest.kt`:

```kotlin
package com.autocalendar.parser

import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptFactoryTest {

    @Test
    fun `prompt embeds today's date`() {
        val prompt = PromptFactory.build("Let's meet tomorrow at 10", LocalDate.of(2026, 9, 24))
        assertTrue("prompt must embed today's date", prompt.contains("2026-09-24"))
        assertTrue(prompt.contains("Let's meet tomorrow at 10"))
    }
}
```

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.parser.PromptFactoryTest"`
Expected: PASS, 1 test.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/autocalendar/parser app/src/test/java/com/autocalendar/parser
git commit -m "feat: Gemini Nano schema, ISO mapper and prompt factory"
```

---

### Task 5: Event-time math

**Files:**
- Create: `app/src/main/java/com/autocalendar/calendar/EventTimes.kt`
- Create: `app/src/test/java/com/autocalendar/calendar/EventTimesTest.kt`

**Interfaces:**
- Consumes: `MeetingDraft` (Task 2).
- Produces: `object EventTimes { fun beginMillis(start: LocalDateTime, zone: ZoneId): Long; fun endMillis(beginMillis: Long, durationMinutes: Int?): Long? }`

- [ ] **Step 1: Write the failing test** `app/src/test/java/com/autocalendar/calendar/EventTimesTest.kt`:

```kotlin
package com.autocalendar.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class EventTimesTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")

    @Test
    fun `begin millis matches local wall clock in zone`() {
        val start = LocalDateTime.of(2026, 9, 25, 15, 0)
        val expected = 1_790_337_600_000L // 2026-09-25 15:00 +03:00 = 12:00 UTC
        assertEquals(expected, EventTimes.beginMillis(start, zone))
    }

    @Test
    fun `end millis for 30 minutes`() {
        val begin = EventTimes.beginMillis(LocalDateTime.of(2026, 9, 25, 15, 0), zone)
        assertEquals(begin + 30 * 60_000L, EventTimes.endMillis(begin, 30))
    }

    @Test
    fun `end millis for 90 minutes`() {
        val begin = EventTimes.beginMillis(LocalDateTime.of(2026, 9, 25, 15, 0), zone)
        assertEquals(begin + 90 * 60_000L, EventTimes.endMillis(begin, 90))
    }

    @Test
    fun `end millis is null when duration absent`() {
        val begin = EventTimes.beginMillis(LocalDateTime.of(2026, 9, 25, 15, 0), zone)
        assertNull(EventTimes.endMillis(begin, null))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.calendar.EventTimesTest"`
Expected: FAIL — `EventTimes` undefined.

- [ ] **Step 3: Implement** `app/src/main/java/com/autocalendar/calendar/EventTimes.kt`:

```kotlin
package com.autocalendar.calendar

import java.time.LocalDateTime
import java.time.ZoneId

object EventTimes {

    fun beginMillis(start: LocalDateTime, zone: ZoneId): Long =
        start.atZone(zone).toInstant().toEpochMilli()

    fun endMillis(beginMillis: Long, durationMinutes: Int?): Long? =
        if (durationMinutes == null) null else beginMillis + durationMinutes * 60_000L
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.calendar.EventTimesTest"`
Expected: PASS, 4 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/autocalendar/calendar app/src/test/java/com/autocalendar/calendar
git commit -m "feat: event-time conversion helpers"
```

---

### Task 6: History storage (Room)

**Files:**
- Create: `app/src/main/java/com/autocalendar/data/ParsedMeetingEntity.kt`
- Create: `app/src/main/java/com/autocalendar/data/ParsedMeetingDao.kt`
- Create: `app/src/main/java/com/autocalendar/data/AutoCalendarDatabase.kt`
- Create: `app/src/main/java/com/autocalendar/data/ParsedMeetingStore.kt`
- Create: `app/src/main/java/com/autocalendar/data/RoomParsedMeetingStore.kt`
- Create: `app/src/test/java/com/autocalendar/data/ParsedMeetingStoreTest.kt`

**Interfaces:**
- Produces:
  - `data class ParsedMeeting(val id: Long, val rawText: String, val title: String, val startMillis: Long, val endMillis: Long?, val location: String?, val createdAt: Long)`
  - `data class NewParsedMeeting(val rawText: String, val title: String, val startMillis: Long, val endMillis: Long?, val location: String?)`
  - `interface ParsedMeetingStore { suspend fun add(meeting: NewParsedMeeting): Long; fun observeAll(): Flow<List<ParsedMeeting>> }`
  - `class RoomParsedMeetingStore(private val dao: ParsedMeetingDao) : ParsedMeetingStore`
  - `@Database(entities = [ParsedMeetingEntity::class], version = 1) abstract class AutoCalendarDatabase : RoomDatabase` with `abstract fun parsedMeetingDao(): ParsedMeetingDao` and companion `fun create(context: Context): AutoCalendarDatabase`.

- [ ] **Step 1: Write the failing test** `app/src/test/java/com/autocalendar/data/ParsedMeetingStoreTest.kt` (Robolectric, in-memory Room) — exercises store + DAO round trip:

```kotlin
package com.autocalendar.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ParsedMeetingStoreTest {

    private lateinit var db: AutoCalendarDatabase
    private lateinit var store: ParsedMeetingStore

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AutoCalendarDatabase::class.java,
        ).build()
        store = RoomParsedMeetingStore(db.parsedMeetingDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `add then observe returns the record`() = runTest {
        store.add(
            NewParsedMeeting(
                rawText = "Let's meet tomorrow at 10",
                title = "Discuss mockup",
                startMillis = 1_790_337_600_000L,
                endMillis = 1_790_337_600_000L + 30 * 60_000L,
                location = "Starbucks",
            ),
        )

        val items = store.observeAll().first()

        assertEquals(1, items.size)
        assertEquals("Discuss mockup", items[0].title)
        assertEquals("Starbucks", items[0].location)
        assertEquals(1_790_337_600_000L, items[0].startMillis)
    }

    @Test
    fun `records come back newest first`() = runTest {
        store.add(NewParsedMeeting("a", "First", 1L, null, null))
        store.add(NewParsedMeeting("b", "Second", 2L, null, null))

        val items = store.observeAll().first()

        assertEquals(listOf("Second", "First"), items.map { it.title })
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.data.ParsedMeetingStoreTest"`
Expected: FAIL — types undefined.

- [ ] **Step 3: Implement entity + DAO + database + store**

`app/src/main/java/com/autocalendar/data/ParsedMeetingEntity.kt`:

```kotlin
package com.autocalendar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parsed_meetings")
data class ParsedMeetingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawText: String,
    val title: String,
    val startMillis: Long,
    val endMillis: Long?,
    val location: String?,
    val createdAt: Long = System.currentTimeMillis(),
)
```

`app/src/main/java/com/autocalendar/data/ParsedMeetingDao.kt`:

```kotlin
package com.autocalendar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ParsedMeetingDao {

    @Insert
    suspend fun insert(entity: ParsedMeetingEntity): Long

    @Query("SELECT * FROM parsed_meetings ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ParsedMeetingEntity>>
}
```

`app/src/main/java/com/autocalendar/data/AutoCalendarDatabase.kt`:

```kotlin
package com.autocalendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ParsedMeetingEntity::class], version = 1, exportSchema = false)
abstract class AutoCalendarDatabase : RoomDatabase() {

    abstract fun parsedMeetingDao(): ParsedMeetingDao

    companion object {
        fun create(context: Context): AutoCalendarDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AutoCalendarDatabase::class.java,
                "autocalendar.db",
            ).build()
    }
}
```

`app/src/main/java/com/autocalendar/data/ParsedMeetingStore.kt`:

```kotlin
package com.autocalendar.data

import kotlinx.coroutines.flow.Flow

data class ParsedMeeting(
    val id: Long,
    val rawText: String,
    val title: String,
    val startMillis: Long,
    val endMillis: Long?,
    val location: String?,
    val createdAt: Long,
)

data class NewParsedMeeting(
    val rawText: String,
    val title: String,
    val startMillis: Long,
    val endMillis: Long?,
    val location: String?,
)

interface ParsedMeetingStore {
    suspend fun add(meeting: NewParsedMeeting): Long
    fun observeAll(): Flow<List<ParsedMeeting>>
}
```

`app/src/main/java/com/autocalendar/data/RoomParsedMeetingStore.kt`:

```kotlin
package com.autocalendar.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomParsedMeetingStore(private val dao: ParsedMeetingDao) : ParsedMeetingStore {

    override suspend fun add(meeting: NewParsedMeeting): Long =
        dao.insert(
            ParsedMeetingEntity(
                rawText = meeting.rawText,
                title = meeting.title,
                startMillis = meeting.startMillis,
                endMillis = meeting.endMillis,
                location = meeting.location,
            ),
        )

    override fun observeAll(): Flow<List<ParsedMeeting>> =
        dao.observeAll().map { entities ->
            entities.map {
                ParsedMeeting(
                    id = it.id,
                    rawText = it.rawText,
                    title = it.title,
                    startMillis = it.startMillis,
                    endMillis = it.endMillis,
                    location = it.location,
                    createdAt = it.createdAt,
                )
            }
        }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.data.ParsedMeetingStoreTest"`
Expected: PASS, 2 tests. (If Robolectric reports a missing SDK, run `sdkmanager "system-images;android-34;google_apis;x86_64"` — but the default Maven-managed Robolectric image usually works.)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/autocalendar/data app/src/test/java/com/autocalendar/data
git commit -m "feat: Room-backed history store"
```

---

### Task 7: Gemini Nano parser implementation (device step)

**Files:**
- Create: `app/src/main/java/com/autocalendar/parser/GeminiNanoParser.kt`

**Interfaces:**
- Consumes: `MeetingParser`, `MeetingTextValidator`, `MeetingDraftMapper`, `PromptFactory`, `DetectedMeeting`, `ParseRequest`, `ParseResult`, `ParseFailureReason` (Tasks 2–4).
- Produces: `class GeminiNanoParser(private val model: GenerativeModel = Generation.getClient()) : MeetingParser`.
- `class GeminiNanoParser` constructor takes a `GenerativeModel` so tests/verification can substitute it; production uses `Generation.getClient()`.

- [ ] **Step 1: Write the parser** `app/src/main/java/com/autocalendar/parser/GeminiNanoParser.kt`:

```kotlin
package com.autocalendar.parser

import com.autocalendar.domain.ParseFailureReason
import com.autocalendar.domain.ParseRequest
import com.autocalendar.domain.ParseResult
import com.google.mlkit.genai.prompt.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.GenAiException
import com.google.mlkit.genai.prompt.IsStructuredOutputFeatureAvailable
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.countTokens
import com.google.mlkit.genai.prompt.generateContent
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.genai.prompt.generateTypedContentRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

class GeminiNanoParser(
    private val model: GenerativeModel = Generation.getClient(),
    private val mapper: MeetingDraftMapper = MeetingDraftMapper(),
    private val maxQuotaRetries: Int = 3,
) : MeetingParser {

    override suspend fun parse(request: ParseRequest): ParseResult {
        when (val status = model.checkStatus()) {
            FeatureStatus.UNAVAILABLE ->
                return ParseResult.Failure(ParseFailureReason.NANO_UNAVAILABLE)

            FeatureStatus.DOWNLOADABLE ->
                model.download().collectLatest { /* wait for DownloadCompleted */ }

            FeatureStatus.DOWNLOADING, FeatureStatus.AVAILABLE -> Unit
        }

        var attempt = 0
        while (true) {
            try {
                return runExtraction(request)
            } catch (e: GenAiException) {
                if (isQuotaError(e) && attempt < maxQuotaRetries) {
                    attempt++
                    delay(1_000L * attempt) // 1s, 2s, 3s backoff
                    continue
                }
                return ParseResult.Failure(ParseFailureReason.QUOTA_EXCEEDED)
            }
        }
    }

    private suspend fun runExtraction(request: ParseRequest): ParseResult {
        val base = generateContentRequest(TextPart(PromptFactory.build(request.rawText, request.today)))

        val detected = if (model.isStructuredOutputFeatureAvailable()) {
            model.generateContent(
                generateTypedContentRequest(base, DetectedMeeting::class),
            ).candidates.firstOrNull()?.response
        } else {
            plainJsonExtraction(request)
        }

        if (detected == null) {
            return ParseResult.Failure(ParseFailureReason.MODEL_PARSE_FAILED)
        }

        val draft = mapper.fromDetected(detected)
            ?: return ParseResult.Failure(ParseFailureReason.MISSING_DATE_OR_TIME)

        return ParseResult.Success(draft)
    }

    private suspend fun plainJsonExtraction(request: ParseRequest): DetectedMeeting? {
        val response = model.generateContent(
            generateContentRequest(
                TextPart(PromptFactory.build(request.rawText, request.today)),
            ) {
                temperature = 0.2f
            },
        ).text ?: return null

        return PlainDetectedMeetingAdapter.fromJson(response)
    }

    private fun isQuotaError(e: GenAiException): Boolean =
        e.errorCode.name.contains("BUSY", ignoreCase = true) ||
            e.errorCode.name.contains("QUOTA", ignoreCase = true) ||
            e.errorCode.name.contains("BATTERY", ignoreCase = true)
}
```

Add the plain-JSON fallback adapter `app/src/main/java/com/autocalendar/parser/PlainDetectedMeetingAdapter.kt`:

```kotlin
package com.autocalendar.parser

import org.json.JSONObject

object PlainDetectedMeetingAdapter {

    fun fromJson(text: String): DetectedMeeting? =
        try {
            val trimmed = text.trim().removePrefix("```json").removeSuffix("```").trim()
            val obj = JSONObject(trimmed)
            DetectedMeeting(
                title = obj.optString("title"),
                date = obj.optString("date"),
                time = obj.optString("time"),
                durationMinutes = obj.optInt("durationMinutes").takeIf { it > 0 },
                location = obj.optString("location").takeIf { it.isNotEmpty() },
            )
        } catch (e: Exception) {
            null
        }
}
```

- [ ] **Step 2: Add a unit test for the JSON adapter** `app/src/test/java/com/autocalendar/parser/PlainDetectedMeetingAdapterTest.kt`:

```kotlin
package com.autocalendar.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlainDetectedMeetingAdapterTest {

    @Test
    fun `parses a clean json response`() {
        val detected = PlainDetectedMeetingAdapter.fromJson(
            """{"title":"Discuss mockup","date":"2026-09-25","time":"15:00","durationMinutes":30,"location":"Starbucks"}""",
        )
        assertEquals("Discuss mockup", detected?.title)
        assertEquals("2026-09-25", detected?.date)
        assertEquals(30, detected?.durationMinutes)
    }

    @Test
    fun `parses a fenced json response`() {
        val detected = PlainDetectedMeetingAdapter.fromJson(
            """```json {"title":"Call","date":"2026-09-25","time":"19:00","durationMinutes":null,"location":null} ```""",
        )
        assertEquals("Call", detected?.title)
        assertNull(detected?.durationMinutes)
        assertNull(detected?.location)
    }

    @Test
    fun `garbage returns null`() {
        assertNull(PlainDetectedMeetingAdapter.fromJson("not json at all"))
    }
}
```

- [ ] **Step 3: Run the adapter tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.parser.PlainDetectedMeetingAdapterTest"`
Expected: PASS, 3 tests.

- [ ] **Step 4: Compile the app**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If the beta ML Kit API signatures differ from the snippets above (names/members drift across beta releases), adjust the imports/calls to the current `com.google.mlkit.genai.prompt` API — the intent (status check → download if needed → structured-or-plain extraction → retry on quota) stays identical. Document any drift in a commit message.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/autocalendar/parser app/src/test/java/com/autocalendar/parser
git commit -m "feat: Gemini Nano parser with structured output and JSON fallback"
```

---

### Task 8: Main screen + share handling

**Files:**
- Create: `app/src/main/java/com/autocalendar/ui/main/MainViewModel.kt`
- Create: `app/src/main/java/com/autocalendar/ui/main/MainScreen.kt`
- Create: `app/src/test/java/com/autocalendar/ui/main/MainViewModelTest.kt`

**Interfaces:**
- Consumes: `MeetingParser`, `MeetingTextValidator`, `ParseRequest`, `ParseResult`, `ParseFailureReason` (Tasks 2–4, 7).
- Produces:
  - `class MainViewModel(parser: MeetingParser, onDraftReady: (MeetingDraft, rawText: String) -> Unit)`
  - State: `val text: StateFlow<String>`, `val isLoading: StateFlow<Boolean>`, `val error: StateFlow<String?>`
  - Actions: `fun onTextChange(value: String)`, `fun parse()`, `fun clearError()`.

- [ ] **Step 1: Write the failing test** `app/src/test/java/com/autocalendar/ui/main/MainViewModelTest.kt` — pins Review Focus 4:

```kotlin
package com.autocalendar.ui.main

import com.autocalendar.domain.MeetingDraft
import com.autocalendar.domain.ParseFailureReason
import com.autocalendar.domain.ParseRequest
import com.autocalendar.domain.ParseResult
import com.autocalendar.parser.MeetingParser
import com.autocalendar.parser.MeetingTextValidator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class MainViewModelTest {

    private class FakeParser(
        private val result: ParseResult,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : MeetingParser {
        var calls = 0
            private set

        override suspend fun parse(request: ParseRequest): ParseResult {
            calls++
            gate?.await()
            return result
        }
    }

    @Test
    fun `valid text calls parser and emits draft`() = runTest {
        val draft = MeetingDraft(
            title = "Discuss mockup",
            startDateTime = LocalDateTime.of(2026, 9, 25, 15, 0),
            durationMinutes = 30,
            location = null,
        )
        val parser = FakeParser(ParseResult.Success(draft))
        var emitted: MeetingDraft? = null
        val vm = MainViewModel(parser, MeetingTextValidator) { draft, _ -> emitted = draft }

        vm.onTextChange("Let's meet on Thursday at 3pm to discuss the mockup.")
        vm.parse()
        advanceUntilIdle()

        assertEquals(1, parser.calls)
        assertEquals(draft, emitted)
        assertEquals(false, vm.isLoading.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `empty text is rejected without calling parser`() = runTest {
        val parser = FakeParser(ParseResult.Failure(ParseFailureReason.TOO_SHORT_TEXT))
        val vm = MainViewModel(parser, MeetingTextValidator) {}

        vm.onTextChange("")
        vm.parse()
        advanceUntilIdle()

        assertEquals(0, parser.calls)
        assertEquals(ParseFailureReason.EMPTY_TEXT.name, vm.error.value)
    }

    @Test
    fun `no-meeting short text is rejected`() = runTest {
        val parser = FakeParser(ParseResult.Failure(ParseFailureReason.TOO_SHORT_TEXT))
        val vm = MainViewModel(parser, MeetingTextValidator) {}

        vm.onTextChange("ok")
        vm.parse()
        advanceUntilIdle()

        assertEquals(0, parser.calls)
        assertEquals(ParseFailureReason.TOO_SHORT_TEXT.name, vm.error.value)
    }

    @Test
    fun `second parse while in flight is ignored`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val parser = FakeParser(ParseResult.Failure(ParseFailureReason.QUOTA_EXCEEDED), gate)
        val vm = MainViewModel(parser, MeetingTextValidator) {}

        vm.onTextChange("Let's meet on Thursday at 3pm to discuss the mockup.")
        vm.parse()
        vm.parse() // second tap while first is still running
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, parser.calls)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.ui.main.MainViewModelTest"`
Expected: FAIL — `MainViewModel` undefined.

- [ ] **Step 3: Implement the ViewModel** `app/src/main/java/com/autocalendar/ui/main/MainViewModel.kt`:

```kotlin
package com.autocalendar.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autocalendar.domain.MeetingDraft
import com.autocalendar.domain.ParseFailureReason
import com.autocalendar.domain.ParseRequest
import com.autocalendar.domain.ParseResult
import com.autocalendar.parser.MeetingParser
import com.autocalendar.parser.MeetingTextValidator
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val parser: MeetingParser,
    private val validator: MeetingTextValidator,
    private val onDraftReady: (MeetingDraft, String) -> Unit,
) : ViewModel() {

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun onTextChange(value: String) {
        _text.value = value
    }

    fun parse() {
        if (_isLoading.value) return

        val failure = validator.validate(_text.value)
        if (failure != null) {
            _error.value = failure.reason.name
            return
        }

        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            val result = parser.parse(ParseRequest(_text.value, LocalDate.now()))
            _isLoading.value = false
            when (result) {
                is ParseResult.Success -> onDraftReady(result.meeting, _text.value)
                is ParseResult.Failure -> _error.value = result.reason.name
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.ui.main.MainViewModelTest"`
Expected: PASS, 4 tests.

- [ ] **Step 5: Implement the Compose screen** `app/src/main/java/com/autocalendar/ui/main/MainScreen.kt`:

```kotlin
package com.autocalendar.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenHistory: () -> Unit,
) {
    val text by viewModel.text.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = viewModel::onTextChange,
            label = { Text("Paste or share a meeting message") },
            modifier = Modifier.fillMaxWidth(),
        )

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = viewModel::parse,
                enabled = !isLoading,
            ) {
                Text("Parse")
            }
            OutlinedButton(onClick = onOpenHistory) {
                Text("History")
            }
        }

        if (isLoading) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.width(24.dp))
                Text("Parsing on device...", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
```

- [ ] **Step 6: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/autocalendar/ui/main app/src/test/java/com/autocalendar/ui/main
git commit -m "feat: main screen and share-text view model"
```

---

### Task 9: Confirmation screen geometry

**Files:**
- Create: `app/src/main/java/com/autocalendar/captured/CalendarIntentBuilder.kt`
- Create: `app/src/main/java/com/autocalendar/captured/CalendarLauncher.kt`
- Create: `app/src/test/java/com/autocalendar/captured/CalendarLauncherTest.kt`

**Interfaces:**
- Consumes: `MeetingDraft`, `EventTimes` (Tasks 2, 5).
- Produces:
  - `data class EventToSave(val title: String, val startMillis: Long, val endMillis: Long?, val location: String?)`
  - `object CalendarIntentBuilder { fun build(meeting: MeetingDraft, zone: ZoneId): EventToSave }`
  - `interface CalendarLauncher { fun launchSafely(intent: Intent): CalendarLaunchOutcome }` with `sealed interface CalendarLaunchOutcome { data object Launched; data object CalendarMissing }`
  - `class AndroidCalendarLauncher(private val context: Context) : CalendarLauncher`

- [ ] **Step 1: Write the failing test** `app/src/test/java/com/autocalendar/captured/CalendarLauncherTest.kt` — pins Review Focus 5:

```kotlin
package com.autocalendar.captured

import android.content.Intent
import com.autocalendar.domain.MeetingDraft
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarLauncherTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")

    @Test
    fun `build fills title begin and end`() {
        val draft = MeetingDraft(
            title = "Discuss mockup",
            startDateTime = LocalDateTime.of(2026, 9, 25, 15, 0),
            durationMinutes = 30,
            location = null,
        )

        val event = CalendarIntentBuilder.build(draft, zone)

        assertEquals("Discuss mockup", event.title)
        assertEquals(1_790_337_600_000L, event.startMillis)
        assertEquals(1_790_337_600_000L + 30 * 60_000L, event.endMillis)
        assertNull(event.location)
    }

    @Test
    fun `build without duration leaves endMillis null`() {
        val draft = MeetingDraft(
            title = "Call",
            startDateTime = LocalDateTime.of(2026, 9, 25, 19, 0),
            durationMinutes = null,
            location = "Park",
        )

        val event = CalendarIntentBuilder.build(draft, zone)

        assertNull(event.endMillis)
        assertEquals("Park", event.location)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.captured.CalendarLauncherTest"`
Expected: FAIL — types undefined.

- [ ] **Step 3: Implement**

`app/src/main/java/com/autocalendar/captured/CalendarIntentBuilder.kt`:

```kotlin
package com.autocalendar.captured

import com.autocalendar.calendar.EventTimes
import com.autocalendar.domain.MeetingDraft
import java.time.ZoneId

data class EventToSave(
    val title: String,
    val startMillis: Long,
    val endMillis: Long?,
    val location: String?,
)

object CalendarIntentBuilder {

    fun build(meeting: MeetingDraft, zone: ZoneId): EventToSave {
        val startMillis = EventTimes.beginMillis(meeting.startDateTime, zone)
        val endMillis = EventTimes.endMillis(startMillis, meeting.durationMinutes)
        return EventToSave(
            title = meeting.title,
            startMillis = startMillis,
            endMillis = endMillis,
            location = meeting.location,
        )
    }
}
```

`app/src/main/java/com/autocalendar/captured/CalendarLauncher.kt`:

```kotlin
package com.autocalendar.captured

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.autocalendar.domain.ParseFailureReason

sealed interface CalendarLaunchOutcome {
    data object Launched : CalendarLaunchOutcome
    data object CalendarMissing : CalendarLaunchOutcome
}

object CalendarEventIntents {
    const val EVENT_INTENT_ACTION = Intent.ACTION_INSERT
    const val EVENT_INTENT_TYPE = "vnd.android.cursor.item/event"
}

interface CalendarLauncher {
    fun launchSafely(intent: Intent): CalendarLaunchOutcome
}

class AndroidCalendarLauncher(private val context: Context) : CalendarLauncher {

    override fun launchSafely(intent: Intent): CalendarLaunchOutcome =
        try {
            context.startActivity(intent)
            CalendarLaunchOutcome.Launched
        } catch (e: ActivityNotFoundException) {
            CalendarLaunchOutcome.CalendarMissing
        }
}

fun EventToSave.toCalendarIntent(): Intent =
    Intent(CalendarEventIntents.EVENT_INTENT_ACTION)
        .setType(CalendarEventIntents.EVENT_INTENT_TYPE)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        .putExtra(CalendarContract.Events.TITLE, title)
        .putExtra(CalendarContract.Events.EVENT_LOCATION, location.orEmpty())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
```

- [ ] **Step 4: Add a Robolectric test for the launcher + intent mapping** `app/src/test/java/com/autocalendar/captured/CalendarLaunchBehaviorTest.kt`:

```kotlin
package com.autocalendar.captured

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalendarLaunchBehaviorTest {

    @Test
    fun `event intent carries title location and times`() {
        val event = EventToSave(
            title = "Discuss mockup",
            startMillis = 1_790_337_600_000L,
            endMillis = 1_790_337_600_000L + 30 * 60_000L,
            location = "Starbucks",
        )

        val intent = event.toCalendarIntent()

        assertEquals(Intent.ACTION_INSERT, intent.action)
        assertEquals("vnd.android.cursor.item/event", intent.type)
        assertEquals("Discuss mockup", intent.getStringExtra(CalendarContract.Events.TITLE))
        assertEquals("Starbucks", intent.getStringExtra(CalendarContract.Events.EVENT_LOCATION))
        assertEquals(1_790_337_600_000L, intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, -1))
        assertEquals(1_790_337_600_000L + 30 * 60_000L, intent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, -1))
    }

    @Test
    fun `launch with no calendar returns CalendarMissing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launcher = AndroidCalendarLauncher(context)

        val outcome = launcher.launchSafely(Intent().setType("vnd.android.cursor.item/event"))

        assertEquals(CalendarLaunchOutcome.CalendarMissing, outcome)
    }
}
```

Note: the Robolectric environment has no calendar handler, so `launchSafely` must catch `ActivityNotFoundException` and return `CalendarMissing` (pins Review Focus 5).

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.captured"`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/autocalendar/captured app/src/test/java/com/autocalendar/captured
git commit -m "feat: calendar event building and safe launch"
```

---

### Task 10: Confirmation screen

**Files:**
- Create: `app/src/main/java/com/autocalendar/ui/confirm/ConfirmViewModel.kt`
- Create: `app/src/main/java/com/autocalendar/ui/confirm/ConfirmScreen.kt`
- Create: `app/src/test/java/com/autocalendar/ui/confirm/ConfirmViewModelTest.kt`

**Interfaces:**
- Consumes: `MeetingDraft`, `CalendarIntentBuilder`, `CalendarLauncher`, `CalendarLaunchOutcome`, `ParsedMeetingStore`, `NewParsedMeeting`, `ParseFailureReason` (Tasks 2, 6, 9).
- Produces:
  - `class ConfirmViewModel(initial: MeetingDraft, rawText: String, zone: ZoneId, intentBuilder: CalendarIntentBuilder, launcher: CalendarLauncher, store: ParsedMeetingStore, onDone: () -> Unit)`
  - State: `title`, `dateText` (`LocalDate.toString()`), `timeText` (`LocalTime.toString()`), `durationText` (blank = none), `locationText`, `error: StateFlow<String?>`
  - Actions: `fun onTitleChange(String)`, `fun onDateTextChange(String)`, `fun onTimeTextChange(String)`, `fun onDurationTextChange(String)`, `fun onLocationTextChange(String)`, `fun createEvent()`.

- [ ] **Step 1: Write the failing test** `app/src/test/java/com/autocalendar/ui/confirm/ConfirmViewModelTest.kt`:

```kotlin
package com.autocalendar.ui.confirm

import com.autocalendar.captured.CalendarIntentBuilder
import com.autocalendar.captured.CalendarLaunchOutcome
import com.autocalendar.captured.CalendarLauncher
import com.autocalendar.captured.EventToSave
import com.autocalendar.data.NewParsedMeeting
import com.autocalendar.data.ParsedMeetingStore
import com.autocalendar.domain.MeetingDraft
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfirmViewModelTest {

    private class FakeLauncher : CalendarLauncher {
        var launched: EventToSave? = null
        var outcome: CalendarLaunchOutcome = CalendarLaunchOutcome.Launched

        override fun launchSafely(intent: android.content.Intent): CalendarLaunchOutcome {
            return outcome
        }
    }

    private class FakeStore : ParsedMeetingStore {
        val added = mutableListOf<NewParsedMeeting>()

        override suspend fun add(meeting: NewParsedMeeting): Long {
            added += meeting
            return added.size.toLong()
        }

        override fun observeAll() = MutableStateFlow(emptyList<com.autocalendar.data.ParsedMeeting>())
    }

    private fun draft(title: String = "Discuss mockup") = MeetingDraft(
        title = title,
        startDateTime = LocalDateTime.of(2026, 9, 25, 15, 0),
        durationMinutes = 30,
        location = "Starbucks",
    )

    @Test
    fun `create event launches calendar and saves history`() = runTest {
        val launcher = FakeLauncher()
        val store = FakeStore()
        var done = false
        val vm = ConfirmViewModel(
            initial = draft(),
            rawText = "Let's meet on Thursday at 3pm.",
            zone = ZoneId.of("Europe/Moscow"),
            intentBuilder = CalendarIntentBuilder,
            launcher = launcher,
            store = store,
            onDone = { done = true },
        )

        vm.createEvent()
        advanceUntilIdle()

        assertEquals(1, store.added.size)
        assertEquals("Discuss mockup", store.added[0].title)
        assertEquals(true, done)
    }

    @Test
    fun `edited fields propagate to saved event`() = runTest {
        val launcher = FakeLauncher()
        val store = FakeStore()
        val vm = ConfirmViewModel(
            initial = draft(),
            rawText = "raw",
            zone = ZoneId.of("Europe/Moscow"),
            intentBuilder = CalendarIntentBuilder,
            launcher = launcher,
            store = store,
            onDone = {},
        )

        vm.onTitleChange("New title")
        vm.onDurationTextChange("60")
        vm.onLocationTextChange("Cafe")
        vm.createEvent()
        advanceUntilIdle()

        val saved = store.added[0]
        assertEquals("New title", saved.title)
        assertEquals(60, (saved.endMillis!! - saved.startMillis) / 60_000L)
        assertEquals("Cafe", saved.location)
    }

    @Test
    fun `missing calendar still saves history`() = runTest {
        val launcher = FakeLauncher().apply { outcome = CalendarLaunchOutcome.CalendarMissing }
        val store = FakeStore()
        var error: String? = null
        val vm = ConfirmViewModel(
            initial = draft(),
            rawText = "raw",
            zone = ZoneId.of("Europe/Moscow"),
            intentBuilder = CalendarIntentBuilder,
            launcher = launcher,
            store = store,
            onDone = {},
        )
        vm.error.collect { error = it }

        vm.createEvent()
        advanceUntilIdle()

        assertEquals(1, store.added.size)
        assertEquals(com.autocalendar.domain.ParseFailureReason.SYSTEM_CALENDAR_MISSING.name, error)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.ui.confirm.ConfirmViewModelTest"`
Expected: FAIL — `ConfirmViewModel` undefined.

- [ ] **Step 3: Implement** `app/src/main/java/com/autocalendar/ui/confirm/ConfirmViewModel.kt`:

```kotlin
package com.autocalendar.ui.confirm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autocalendar.captured.CalendarIntentBuilder
import com.autocalendar.captured.CalendarLaunchOutcome
import com.autocalendar.captured.CalendarLauncher
import com.autocalendar.captured.toCalendarIntent
import com.autocalendar.data.NewParsedMeeting
import com.autocalendar.data.ParsedMeetingStore
import com.autocalendar.domain.MeetingDraft
import com.autocalendar.domain.ParseFailureReason
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ConfirmViewModel(
    initial: MeetingDraft,
    private val rawText: String,
    private val zone: ZoneId,
    private val intentBuilder: CalendarIntentBuilder,
    private val launcher: CalendarLauncher,
    private val store: ParsedMeetingStore,
    private val onDone: () -> Unit,
) : ViewModel() {

    var title by mutableStateOf(initial.title)
        private set
    var dateText by mutableStateOf(initial.startDateTime.toLocalDate().toString())
        private set
    var timeText by mutableStateOf(initial.startDateTime.toLocalTime().toString())
        private set
    var durationText by mutableStateOf(initial.durationMinutes?.toString() ?: "")
        private set
    var locationText by mutableStateOf(initial.location ?: "")
        private set

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun onTitleChange(value: String) { title = value }
    fun onDateTextChange(value: String) { dateText = value }
    fun onTimeTextChange(value: String) { timeText = value }
    fun onDurationTextChange(value: String) { durationText = value }
    fun onLocationTextChange(value: String) { locationText = value }

    fun createEvent() {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            _error.value = "Title is required"
            return
        }
        val start = parseStart() ?: run {
            _error.value = ParseFailureReason.MISSING_DATE_OR_TIME.name
            return
        }
        val duration = durationText.toIntOrNull()?.takeIf { it > 0 }
        val meeting = MeetingDraft(
            title = trimmedTitle,
            startDateTime = start,
            durationMinutes = duration,
            location = locationText.trim().takeIf { it.isNotEmpty() },
        )
        val event = intentBuilder.build(meeting, zone)
        _error.value = null

        viewModelScope.launch {
            store.add(
                NewParsedMeeting(
                    rawText = rawText,
                    title = event.title,
                    startMillis = event.startMillis,
                    endMillis = event.endMillis,
                    location = event.location,
                ),
            )
            when (launcher.launchSafely(event.toCalendarIntent())) {
                CalendarLaunchOutcome.Launched -> onDone()
                CalendarLaunchOutcome.CalendarMissing ->
                    _error.value = ParseFailureReason.SYSTEM_CALENDAR_MISSING.name
            }
        }
    }

    private fun parseStart(): LocalDateTime? {
        val date = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull() ?: return null
        val time = runCatching { LocalTime.parse(timeText.trim()) }.getOrNull() ?: return null
        return LocalDateTime.of(date, time)
    }
}
```

- [ ] **Step 4: Implement the screen** `app/src/main/java/com/autocalendar/ui/confirm/ConfirmScreen.kt`:

```kotlin
package com.autocalendar.ui.confirm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConfirmScreen(
    viewModel: ConfirmViewModel,
    onCancel: () -> Unit,
) {
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Review the meeting", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = viewModel.title,
            onValueChange = viewModel::onTitleChange,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = viewModel.dateText,
            onValueChange = viewModel::onDateTextChange,
            label = { Text("Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = viewModel.timeText,
            onValueChange = viewModel::onTimeTextChange,
            label = { Text("Start time (HH:mm)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = viewModel.durationText,
            onValueChange = viewModel::onDurationTextChange,
            label = { Text("Duration minutes (blank = not set)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = viewModel.locationText,
            onValueChange = viewModel::onLocationTextChange,
            label = { Text("Location (blank = not set)") },
            modifier = Modifier.fillMaxWidth(),
        )

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::createEvent) {
                Text("Create event")
            }
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.ui.confirm.ConfirmViewModelTest"`
Expected: PASS, 3 tests.

- [ ] **Step 6: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/autocalendar/ui/confirm app/src/test/java/com/autocalendar/ui/confirm
git commit -m "feat: confirmation screen with editable fields"
```

---

### Task 11: History screen

**Files:**
- Create: `app/src/main/java/com/autocalendar/ui/history/HistoryViewModel.kt`
- Create: `app/src/main/java/com/autocalendar/ui/history/HistoryScreen.kt`
- Create: `app/src/test/java/com/autocalendar/ui/history/HistoryViewModelTest.kt`

**Interfaces:**
- Consumes: `ParsedMeetingStore`, `ParsedMeeting` (Task 6).
- Produces:
  - `class HistoryViewModel(store: ParsedMeetingStore, onSelect: (ParsedMeeting) -> Unit)`
  - `val items: StateFlow<List<ParsedMeeting>>`, `fun select(item: ParsedMeeting)`.

- [ ] **Step 1: Write the failing test** `app/src/test/java/com/autocalendar/ui/history/HistoryViewModelTest.kt`:

```kotlin
package com.autocalendar.ui.history

import com.autocalendar.data.ParsedMeeting
import com.autocalendar.data.ParsedMeetingStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryViewModelTest {

    private class FakeStore(items: List<ParsedMeeting>) : ParsedMeetingStore {
        private val flow = MutableStateFlow(items)

        override suspend fun add(meeting: com.autocalendar.data.NewParsedMeeting): Long = 0L
        override fun observeAll() = flow
    }

    private fun item(id: Long, title: String) = ParsedMeeting(
        id = id,
        rawText = "raw",
        title = title,
        startMillis = 1L,
        endMillis = null,
        location = null,
        createdAt = id,
    )

    @Test
    fun `items are exposed from the store`() = runTest {
        val store = FakeStore(listOf(item(2, "Second"), item(1, "First")))
        val vm = HistoryViewModel(store) {}

        assertEquals(listOf("Second", "First"), vm.items.value.map { it.title })
    }

    @Test
    fun `select re-emits the tapped item`() = runTest {
        val store = FakeStore(listOf(item(1, "First")))
        var selected: ParsedMeeting? = null
        val vm = HistoryViewModel(store) { selected = it }

        vm.select(item(1, "First"))

        assertEquals(1L, selected?.id)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.ui.history.HistoryViewModelTest"`
Expected: FAIL — `HistoryViewModel` undefined.

- [ ] **Step 3: Implement** `app/src/main/java/com/autocalendar/ui/history/HistoryViewModel.kt`:

```kotlin
package com.autocalendar.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autocalendar.data.ParsedMeeting
import com.autocalendar.data.ParsedMeetingStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
    store: ParsedMeetingStore,
    private val onSelect: (ParsedMeeting) -> Unit,
) : ViewModel() {

    val items: StateFlow<List<ParsedMeeting>> =
        store.observeAll().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    fun select(item: ParsedMeeting) {
        onSelect(item)
    }
}
```

- [ ] **Step 4: Implement the screen** `app/src/main/java/com/autocalendar/ui/history/HistoryScreen.kt`:

```kotlin
package com.autocalendar.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autocalendar.data.ParsedMeeting
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
) {
    val items by viewModel.items.collectAsState()
    val zone = ZoneId.systemDefault()

    Column(Modifier.fillMaxSize()) {
        Text(
            "History",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )
        if (items.isEmpty()) {
            Text(
                "No saved meetings yet.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn {
                items(items, key = { it.id }) { item ->
                    HistoryRow(item, zone, onClick = { viewModel.select(item) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: ParsedMeeting,
    zone: ZoneId,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(item.title, style = MaterialTheme.typography.titleMedium)
        Text(
            Instant.ofEpochMilli(item.startMillis).atZone(zone).format(displayFormatter),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        item.location?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autocalendar.ui.history.HistoryViewModelTest"`
Expected: PASS, 2 tests.

- [ ] **Step 6: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/autocalendar/ui/history app/src/test/java/com/autocalendar/ui/history
git commit -m "feat: history screen"
```

---

### Task 12: Wire it together (navigation, DI, share intent)

**Files:**
- Modify: `app/src/main/java/com/autocalendar/AutoCalendarApp.kt`
- Modify: `app/src/main/java/com/autocalendar/MainActivity.kt`
- Create: `app/src/main/java/com/autocalendar/AppContainer.kt`
- Create: `app/src/main/java/com/autocalendar/ui/AppNav.kt`
- Modify: `app/src/main/AndroidManifest.xml` (already has SEND filter from Task 1 — no change needed)

**Interfaces:**
- Consumes: everything from Tasks 1–11.
- Produces: the running app: share text starts at Main screen; successful parse navigates to Confirm; "Create event" opens the calendar and saves history; History screen re-opens a saved record in Confirm.
- `data class ConfirmRequest(val draft: MeetingDraft, val rawText: String)` living in `AppContainer.kt`; a single `MutableStateFlow<ConfirmRequest?>` drives both incoming shares and history recalls.

- [ ] **Step 1: Implement the AppContainer** `app/src/main/java/com/autocalendar/AppContainer.kt`:

```kotlin
package com.autocalendar

import android.content.Context
import com.autocalendar.captured.AndroidCalendarLauncher
import com.autocalendar.captured.CalendarLauncher
import com.autocalendar.data.AutoCalendarDatabase
import com.autocalendar.data.ParsedMeetingStore
import com.autocalendar.data.RoomParsedMeetingStore
import com.autocalendar.domain.MeetingDraft
import com.autocalendar.parser.GeminiNanoParser
import com.autocalendar.parser.MeetingParser
import kotlinx.coroutines.flow.MutableStateFlow

data class ConfirmRequest(
    val draft: MeetingDraft,
    val rawText: String,
)

class AppContainer(context: Context) {

    private val database = AutoCalendarDatabase.create(context)

    val parser: MeetingParser = GeminiNanoParser()
    val store: ParsedMeetingStore = RoomParsedMeetingStore(database.parsedMeetingDao())
    val launcher: CalendarLauncher = AndroidCalendarLauncher(context)
    val pendingSharedText = MutableStateFlow<String?>(null)
    val confirmRequest = MutableStateFlow<ConfirmRequest?>(null)
}
```

- [ ] **Step 2: Update the Application class** `app/src/main/java/com/autocalendar/AutoCalendarApp.kt`:

```kotlin
package com.autocalendar

import android.app.Application

class AutoCalendarApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

- [ ] **Step 3: Implement navigation and factories** `app/src/main/java/com/autocalendar/ui/AppNav.kt`:

```kotlin
package com.autocalendar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.autocalendar.AppContainer
import com.autocalendar.ConfirmRequest
import com.autocalendar.domain.MeetingDraft
import com.autocalendar.parser.MeetingParser
import com.autocalendar.parser.MeetingTextValidator
import com.autocalendar.ui.confirm.ConfirmScreen
import com.autocalendar.ui.confirm.ConfirmViewModel
import com.autocalendar.ui.history.HistoryScreen
import com.autocalendar.ui.history.HistoryViewModel
import com.autocalendar.ui.main.MainScreen
import com.autocalendar.ui.main.MainViewModel
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object Routes {
    const val MAIN = "main"
    const val CONFIRM = "confirm"
    const val HISTORY = "history"
}

@Composable
fun AppNav(
    pendingSharedText: MutableStateFlow<String?>,
    confirmRequest: StateFlow<ConfirmRequest?>,
    createMain: () -> MainViewModel,
    createConfirm: (ConfirmRequest) -> ConfirmViewModel,
    createHistory: () -> HistoryViewModel,
) {
    val nav = rememberNavController()
    val request by confirmRequest.collectAsState()

    LaunchedEffect(request) {
        if (request != null) {
            nav.navigate(Routes.CONFIRM)
        }
    }

    NavHost(navController = nav, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            val viewModel = remember { createMain() }
            val pending by pendingSharedText.collectAsState()
            LaunchedEffect(pending) {
                if (pending != null) {
                    viewModel.onTextChange(pending)
                    pendingSharedText.value = null
                }
            }
            MainScreen(
                viewModel = viewModel,
                onOpenHistory = { nav.navigate(Routes.HISTORY) },
            )
        }
        composable(Routes.CONFIRM) {
            val current = request
            if (current != null) {
                val viewModel = remember(current) { createConfirm(current) }
                ConfirmScreen(
                    viewModel = viewModel,
                    onCancel = {
                        confirmRequest.value = null
                        nav.popBackStack()
                    },
                )
            }
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                viewModel = createHistory(),
                onBack = { nav.popBackStack() },
            )
        }
    }
}

fun createMainViewModel(
    parser: MeetingParser,
    onDraftReady: (MeetingDraft, String) -> Unit,
): MainViewModel = MainViewModel(parser, MeetingTextValidator, onDraftReady)

fun createConfirmViewModel(
    container: AppContainer,
    request: ConfirmRequest,
    onDone: () -> Unit,
): ConfirmViewModel =
    ConfirmViewModel(
        initial = request.draft,
        rawText = request.rawText,
        zone = ZoneId.systemDefault(),
        intentBuilder = com.autocalendar.captured.CalendarIntentBuilder,
        launcher = container.launcher,
        store = container.store,
        onDone = onDone,
    )

fun createHistoryViewModel(
    container: AppContainer,
    onSelect: (com.autocalendar.data.ParsedMeeting) -> Unit,
): HistoryViewModel = HistoryViewModel(container.store, onSelect)
```

- [ ] **Step 4: Update MainActivity to own navigation and share handling** `app/src/main/java/com/autocalendar/MainActivity.kt`:

```kotlin
package com.autocalendar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.autocalendar.data.toDraft
import com.autocalendar.ui.AppNav
import com.autocalendar.ui.confirm.ConfirmViewModel
import com.autocalendar.ui.createConfirmViewModel
import com.autocalendar.ui.createHistoryViewModel
import com.autocalendar.ui.createMainViewModel
import com.autocalendar.ui.history.HistoryViewModel
import com.autocalendar.ui.main.MainViewModel

class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy { (application as AutoCalendarApp).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleSharedText(intent)

        setContent {
            MaterialTheme {
                AppNav(
                    pendingSharedText = container.pendingSharedText,
                    confirmRequest = container.confirmRequest,
                    createMain = ::buildMainViewModel,
                    createConfirm = ::buildConfirmViewModel,
                    createHistory = ::buildHistoryViewModel,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedText(intent)
    }

    private fun handleSharedText(intent: Intent?) {
        val shared = intent?.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (shared.isEmpty()) return
        container.pendingSharedText.value = shared
    }

    private fun buildMainViewModel(): MainViewModel =
        createMainViewModel(
            parser = container.parser,
            onDraftReady = { draft, rawText ->
                container.confirmRequest.value = ConfirmRequest(draft, rawText)
            },
        )

    private fun buildConfirmViewModel(request: ConfirmRequest): ConfirmViewModel =
        createConfirmViewModel(
            container = container,
            request = request,
            onDone = {
                container.confirmRequest.value = null
                finish()
            },
        )

    private fun buildHistoryViewModel(): HistoryViewModel =
        createHistoryViewModel(
            container = container,
            onSelect = { item ->
                container.confirmRequest.value = ConfirmRequest(item.toDraft(), "recalled")
            },
        )
}
```

- [ ] **Step 5: Add a pure helper for history recall**

Create `app/src/main/java/com/autocalendar/data/ParsedMeetingExt.kt`:

```kotlin
package com.autocalendar.data

import com.autocalendar.domain.MeetingDraft
import java.time.Instant
import java.time.ZoneId

fun ParsedMeeting.toDraft(zone: ZoneId = ZoneId.systemDefault()): MeetingDraft =
    MeetingDraft(
        title = title,
        startDateTime = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDateTime(),
        durationMinutes = endMillis?.let { ((it - startMillis) / 60_000L).toInt() },
        location = location,
    )
```

Note: re-creating an event from history uses the saved post-edit times (spec §4.5: "re-open the confirmation screen with that data"). The draft title/location/duration derive from the stored record.

- [ ] **Step 6: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS — all tasks' tests green.

- [ ] **Step 8: Commit**

```bash
git add .
git commit -m "feat: wire navigation, DI and share intent handling"
```

---

### Task 13: On-device verification on Pixel 10 Pro

**Files:**
- No code changes expected unless verification surfaces bugs; fix those as small follow-up commits.

**Interfaces:**
- Consumes: the assembled app from Tasks 1–12.

- [ ] **Step 1: Prepare the device**

Enroll in AICore if not already: join `aicore-experimental` Google group, opt into the AICore `com.google.android.aicore` testing program, update AICore via system services, and download the Gemini Nano model (see the AICore Developer Preview docs). On Pixel 10 Pro this is the nano-v3 model.

- [ ] **Step 2: Install the debug build**

Run: `./gradlew :app:installDebug`
Expected: installed on the device.

- [ ] **Step 3: Run the E2E happy path**

1. In a messenger, compose: "Давай встретимся обсудить макет, в четверг в 15:00 у стабакса" (or similar Russian/English living-language text).
2. Share it → AutoCalendar. Main screen shows the text.
3. Tap "Parse". Expect a ~1–10s on-device inference, then navigation to Confirm with a sensible title/date/time.
4. Fix anything, tap "Create event". Expect the calendar picker to open, the event to appear, the app to close.
5. Open the app, tap "History. Expect the saved record; tap it → Confirm re-opens with the record's values.

- [ ] **Step 4: Verify relative dates and multi-message text**

1. Share "созвон по квартире в следующую пятницу, в 19 часов" → the resolved date must be *next Friday from today*, not today.
2. Share 2–3 selected messages including one you wrote yourself → a single meeting is extracted.

- [ ] **Step 5: Verify failure paths**

1. Share "ok" → the app must show the TOO_SHORT error, no model call.
2. Share a date with no time → MISSING_DATE_OR_TIME error on Confirm (or error from parser).
3. Turn on airplane mode → parse must still work (fully on-device).

- [ ] **Step 6: Fix anything found, re-run, and record results**

If any step fails, apply a fix with TDD (new/changed test first), re-run `./gradlew :app:testDebugUnitTest`, re-verify on device, and commit. Leave a short `docs/verification-notes.md` in the repo with the dates/results of each scenario.

---

## Self-Review Summary

- **Spec coverage:** Goal (§1) → Tasks 1–13. Scenario (§3) → Task 12 wiring + Task 13 device run. Components (§4) → Task 4 (parser/schema) + Task 7 (GeminiNanoParser), Tasks 8–11 (UI/VM), Task 9 (calendar), Task 6 (history). Stack (§5) → Task 1. Testing (§6) → Tasks 2–11 unit tests + Task 13 device checks. Errors (§7) → Task 3 (empty/short), Task 4 (missing date/time), Task 7 (NANO_UNAVAILABLE/QUOTA), Task 9/10 (calendar missing). Future (§8) → `MeetingParser` interface keeps the seam; nothing extra built. Non-goals (§9) → no cloud, no auto-create, no voice, no multi-session context.
- **Type consistency:** `MeetingDraft`/`ParseRequest`/`ParseResult`/`ParseFailureReason` defined in Task 2 and consumed verbatim in Tasks 3–12; `EventTimes` (Task 5) consumed by `CalendarIntentBuilder` (Task 9); `ParsedMeetingStore`/`NewParsedMeeting`/`ParsedMeeting` (Task 6) consumed by Tasks 10–12; `DetectedMeeting` mapper (Task 4) consumed by parser (Task 7). The `SYSTEM_CALENDAR_MISSING` reason is only raised at the UI layer (Task 10) — acceptable, it holds no state.
- **Review Focus:** #1,#2 → Task 3 tests; #3 → Task 4 mapper test; #4 → Task 8 in-flight guard test; #5 → Task 9 launcher test (no calendar) + Task 10 missing-calendar-saves-history test. All pinned.
- **Known risk to monitor during execution:** the ML Kit GenAI Prompt API is `1.0.0-beta4`; exact helper names (`generateTypedContentRequest`, `isStructuredOutputFeatureAvailable`, `countTokens`) may drift. Task 7 Step 4 explicitly instructs adapting to the current beta API surface while preserving behavior. Keep the `DetectedMeeting` keep-rule in ProGuard (Task 1 Step 7).