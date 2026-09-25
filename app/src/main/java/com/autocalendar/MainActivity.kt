package com.autocalendar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.autocalendar.calendar.CalendarLaunchOutcome
import com.autocalendar.calendar.EventTimes
import com.autocalendar.calendar.EventToSave
import com.autocalendar.data.NewParsedMeeting
import com.autocalendar.data.toDraft
import com.autocalendar.domain.MeetingDraft
import com.autocalendar.ui.AppNav
import com.autocalendar.ui.confirm.ConfirmViewModel
import com.autocalendar.ui.createConfirmViewModel
import com.autocalendar.ui.createHistoryViewModel
import com.autocalendar.ui.createMainViewModel
import com.autocalendar.ui.history.HistoryViewModel
import com.autocalendar.ui.main.MainViewModel
import java.time.ZoneId
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy { (application as AutoCalendarApp).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleSharedText(intent)
        }

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
        setIntent(intent)
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
                launchEditedCalendar(draft, rawText, finishOnDone = true)
            },
        )

    private fun buildConfirmViewModel(request: ConfirmRequest): ConfirmViewModel =
        createConfirmViewModel(
            container = container,
            request = request,
            onDone = {
                container.confirmRequest.value = null
                if (request.finishOnDone) finish()
            },
        )

    private fun buildHistoryViewModel(): HistoryViewModel =
        createHistoryViewModel(
            container = container,
            onSelect = { item ->
                launchEditedCalendar(item.toDraft(), item.rawText, finishOnDone = false)
            },
        )

    private fun launchEditedCalendar(draft: MeetingDraft, rawText: String, finishOnDone: Boolean) {
        val beginMillis = EventTimes.beginMillis(draft.startDateTime, ZoneId.systemDefault())
        val event = EventToSave(
            title = draft.title,
            beginMillis = beginMillis,
            endMillis = draft.durationMinutes?.let { beginMillis + it * 60_000L },
            location = null,
        )
        when (val outcome = container.launcher.launch(event)) {
            is CalendarLaunchOutcome.Success -> {
                recordHistory(rawText, event)
                if (finishOnDone) finish()
            }
            is CalendarLaunchOutcome.Failure -> {
                container.confirmRequest.value = ConfirmRequest(draft, rawText, finishOnDone)
            }
        }
    }

    private fun recordHistory(rawText: String, event: EventToSave) {
        container.appScope.launch {
            try {
                container.store.add(
                    NewParsedMeeting(
                        rawText = rawText,
                        title = event.title,
                        startMillis = event.beginMillis,
                        endMillis = event.endMillis,
                        location = event.location,
                    ),
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
            }
        }
    }
}
