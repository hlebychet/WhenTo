### Task 12: Wire it together (navigation, DI, share intent) — ADAPTED TO ACTUAL CODE

CONTROLLER RULING (see ledger `Task 12: Ruling`): the plan's original Task 12 text targets an
architecture that differs from the committed Tasks 1-11 code and cannot compile verbatim.
This brief is the authoritative, adapted spec. Transcribe every code block BELOW exactly.

**Files:**
- Create: `app/src/main/java/com/autocalendar/AppContainer.kt`
- Modify: `app/src/main/java/com/autocalendar/AutoCalendarApp.kt`
- Create: `app/src/main/java/com/autocalendar/ui/AppNav.kt`
- Modify: `app/src/main/java/com/autocalendar/MainActivity.kt`
- Create: `app/src/main/java/com/autocalendar/data/ParsedMeetingExt.kt`
- Modify: `app/src/main/java/com/autocalendar/ui/main/MainScreen.kt`
- Modify: `app/src/main/java/com/autocalendar/ui/confirm/ConfirmScreen.kt`
- Modify: `app/src/main/java/com/autocalendar/ui/confirm/ConfirmViewModel.kt`
- NOT touched: `app/src/main/AndroidManifest.xml` (SEND filter exists), any build/gradle file, anything else.

**Interfaces:**
- Consumes: everything from Tasks 1-11 (actual signatures).
- Produces the running app per spec §3: share text starts at Main screen; successful parse
  navigates to Confirm (draft pre-filled); "Create event" opens the system calendar and saves
  history; History screen re-opens a saved record in Confirm.
- `data class ConfirmRequest(val draft: MeetingDraft, val rawText: String)` in `AppContainer.kt`;
  a single `MutableStateFlow<ConfirmRequest?>` drives both incoming shares and history recalls.

---

- [ ] **Step 1: `app/src/main/java/com/autocalendar/AppContainer.kt`:**

```kotlin
package com.autocalendar

import android.content.Context
import com.autocalendar.calendar.AndroidCalendarLauncher
import com.autocalendar.calendar.CalendarLauncher
import com.autocalendar.data.AutoCalendarDatabase
import com.autocalendar.data.ParsedMeetingStore
import com.autocalendar.data.RoomParsedMeetingStore
import com.autocalendar.domain.MeetingDraft
import com.autocalendar.parser.GeminiNanoParser
import com.autocalendar.parser.MeetingParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val pendingSharedText = MutableStateFlow<String?>(null)
    val confirmRequest = MutableStateFlow<ConfirmRequest?>(null)
}
```

- [ ] **Step 2: Update `app/src/main/java/com/autocalendar/AutoCalendarApp.kt`:**

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

- [ ] **Step 3: `app/src/main/java/com/autocalendar/ui/AppNav.kt`:**

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
import com.autocalendar.data.NewParsedMeeting
import com.autocalendar.data.ParsedMeeting
import com.autocalendar.domain.MeetingDraft
import com.autocalendar.parser.MeetingParser
import com.autocalendar.parser.MeetingTextValidator
import com.autocalendar.ui.confirm.ConfirmScreen
import com.autocalendar.ui.confirm.ConfirmViewModel
import com.autocalendar.ui.history.HistoryScreen
import com.autocalendar.ui.history.HistoryViewModel
import com.autocalendar.ui.main.MainScreen
import com.autocalendar.ui.main.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

object Routes {
    const val MAIN = "main"
    const val CONFIRM = "confirm"
    const val HISTORY = "history"
}

@Composable
fun AppNav(
    pendingSharedText: MutableStateFlow<String?>,
    confirmRequest: MutableStateFlow<ConfirmRequest?>,
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
            val viewModel = remember { createHistory() }
            HistoryScreen(
                viewModel = viewModel,
                onBack = { nav.popBackStack() },
            )
        }
    }
}

fun createMainViewModel(
    parser: MeetingParser,
    onDraftReady: (MeetingDraft, String) -> Unit,
): MainViewModel = MainViewModel(parser, { MeetingTextValidator.validate(it) }, onDraftReady)

fun createConfirmViewModel(
    container: AppContainer,
    request: ConfirmRequest,
    onDone: () -> Unit,
): ConfirmViewModel {
    val viewModel = ConfirmViewModel(
        launcher = container.launcher,
        onEventSaved = { event ->
            container.appScope.launch {
                container.store.add(
                    NewParsedMeeting(
                        rawText = request.rawText,
                        title = event.title,
                        startMillis = event.beginMillis,
                        endMillis = event.endMillis,
                        location = event.location,
                    ),
                )
            }
            onDone()
        },
    )
    viewModel.onDraftReady(request.draft, request.rawText)
    return viewModel
}

fun createHistoryViewModel(
    container: AppContainer,
    onSelect: (ParsedMeeting) -> Unit,
): HistoryViewModel = HistoryViewModel(container.store, onSelect)
```

Note: `toDraft` (used here) is supplied by Step 5 below. `add` is suspend; the wiring launches it on
`container.appScope` after a successful calendar launch, then calls `onDone()` (per spec §4.5 a
history record is written only when the event was actually created).

- [ ] **Step 4: Update `app/src/main/java/com/autocalendar/MainActivity.kt`:**

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
                container.confirmRequest.value = ConfirmRequest(item.toDraft(), item.rawText)
            },
        )
}
```

- [ ] **Step 5: `app/src/main/java/com/autocalendar/data/ParsedMeetingExt.kt`:**

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

- [ ] **Step 6: Update `app/src/main/java/com/autocalendar/ui/main/MainScreen.kt`** — replace the whole file:

```kotlin
package com.autocalendar.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenHistory: () -> Unit,
) {
    val text by viewModel.text.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("AutoCalendar", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Paste a message to extract a meeting",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { viewModel.onTextChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Message text") },
                    singleLine = false,
                    minLines = 4,
                    enabled = !isLoading,
                )
                Button(
                    onClick = { viewModel.parse() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && text.isNotBlank(),
                ) {
                    Text(if (isLoading) "Parsing..." else "Extract Meeting")
                }
            }
        }

        error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }

        TextButton(onClick = onOpenHistory) {
            Text("History")
        }
    }
}
```

- [ ] **Step 7: Update `app/src/main/java/com/autocalendar/ui/confirm/ConfirmScreen.kt`** — replace the whole file:

```kotlin
package com.autocalendar.ui.confirm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmScreen(
    viewModel: ConfirmViewModel,
    onCancel: () -> Unit,
) {
    val title by viewModel.title.collectAsState()
    val location by viewModel.location.collectAsState()
    val startMillis by viewModel.startMillis.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Confirm Event", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title ?: "",
                    onValueChange = { viewModel.onTitleChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = location ?: "",
                    onValueChange = { viewModel.onLocationChange(it.ifBlank { null }) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Location (optional)") },
                    singleLine = true,
                )
                if (startMillis != null) {
                    Text(
                        Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).format(displayFormatter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                error?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }
                Button(
                    onClick = { viewModel.onCreateClick() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Create Event")
                }
                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
```

- [ ] **Step 8: Update `app/src/main/java/com/autocalendar/ui/confirm/ConfirmViewModel.kt`** — ADD two setters
(keep every existing member unchanged, including `onDraftReady`, `onCreateClick`, `onCancel`, `clear`). Insert
immediately after `onDraftReady`:

```kotlin
    fun onTitleChange(value: String) {
        _title.value = value
        _error.value = null
    }

    fun onLocationChange(value: String?) {
        _location.value = value
        _error.value = null
    }
```

- [ ] **Step 9: Compile**

Run (from the worktree root, pwsh; the JAVA_HOME prefix is mandatory — Gradle 8.13 cannot run on the system JDK 24/25):
`$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 10: Run the full unit test suite**

`$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:testDebugUnitTest`
Expected: PASS — all tasks' tests green (existing MainViewModelTest, ConfirmViewModelTest, CalendarLauncherTest, HistoryViewModelTest, mappers, etc.).

- [ ] **Step 11: Commit** (exact commands, worktree root)

```bash
git add .
git commit -m "feat: wire navigation, DI and share intent handling"
```

The commit may contain ONLY the files listed at the top (git add . must not accidentally stage anything
else; check `git status` before committing; the report file and ledger live in a git-ignored dir).

---

Global constraints: package root `com.autocalendar`; English code/UI strings/commit messages; **NO EMOJIS**
anywhere (code, UI strings, comments — ASCII and typographic punctuation only); do not touch build files,
`gradle/libs.versions.toml`, the AndroidManifest, or files outside the list above; do not modify the behavior
of `MainViewModel`, `HistoryViewModel`, `ParsedMeetingStore`, the parser, or the calendar package except where
this brief explicitly does.