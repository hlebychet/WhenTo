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

