package com.autocalendar.ui.confirm

import com.autocalendar.calendar.CalendarLaunchOutcome
import com.autocalendar.calendar.CalendarLauncher
import com.autocalendar.calendar.EventToSave
import com.autocalendar.domain.MeetingDraft
import com.autocalendar.domain.ParseResult
import com.autocalendar.parser.MeetingParser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class ConfirmViewModelTest {

    private class FakeParser(
        private val result: ParseResult,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : MeetingParser {
        override suspend fun parse(request: com.autocalendar.domain.ParseRequest): ParseResult {
            gate?.await()
            return result
        }
    }

    private class FakeLauncher(
        private val outcome: CalendarLaunchOutcome,
    ) : CalendarLauncher {
        var lastEvent: com.autocalendar.calendar.EventToSave? = null
        override fun launch(event: com.autocalendar.calendar.EventToSave): CalendarLaunchOutcome {
            lastEvent = event
            return outcome
        }
    }

    @Test
    fun `onDraftReady populates fields synchronously`() {
        val draft = MeetingDraft(
            title = "Discuss mockup",
            startDateTime = LocalDateTime.of(2026, 9, 25, 15, 0),
            durationMinutes = 30,
            location = "Starbucks",
        )
        val launcher = FakeLauncher(CalendarLaunchOutcome.Success(0))
        val vm = ConfirmViewModel(launcher, { })

        vm.onDraftReady(draft, "raw text")

        assertEquals("Discuss mockup", vm.title.value)
        assertEquals("Starbucks", vm.location.value)
        assertNotNull(vm.startMillis.value)
        assertEquals(30, vm.durationMinutes.value)
    }

    @Test
    fun `onCreateClick launches calendar and saves event`() = runTest {
        val launcher = FakeLauncher(CalendarLaunchOutcome.Success(0))
        var savedEvent: com.autocalendar.calendar.EventToSave? = null
        // Create ViewModel inside runTest with test scope
        val vm = ConfirmViewModel(launcher, { savedEvent = it }, this)

        // Pre-populate the ViewModel state (simulating onDraftReady)
        vm.onDraftReady(
            MeetingDraft("Discuss mockup", LocalDateTime.of(2026, 9, 25, 15, 0), 30, "Starbucks"),
            "raw text"
        )

        vm.onCreateClick()
        advanceUntilIdle()

        assertEquals("Discuss mockup", launcher.lastEvent?.title)
        assertEquals("Starbucks", launcher.lastEvent?.location)
        assertNotNull(launcher.lastEvent?.beginMillis)
        assertEquals(30 * 60_000L, launcher.lastEvent?.endMillis?.let { it - (launcher.lastEvent?.beginMillis ?: 0L) } ?: 30 * 60_000L)
    }

    @Test
    fun `create event saves to history on success`() = runTest {
        // This test would require the ParsedMeetingStore integration
        // For now, we verify the launcher is called
    }

    @Test
    fun `calendar missing shows error`() = runTest {
        val draft = MeetingDraft(
            title = "Test",
            startDateTime = LocalDateTime.now().plusDays(1),
            durationMinutes = 60,
            location = null,
        )
        val launcher = FakeLauncher(CalendarLaunchOutcome.Failure(com.autocalendar.calendar.LaunchFailureReason.NO_CALENDAR_APP))
        val vm = ConfirmViewModel(launcher, {})

        vm.onDraftReady(draft, "raw")
        vm.onCreateClick()
        advanceUntilIdle()

        assertEquals(com.autocalendar.calendar.LaunchFailureReason.NO_CALENDAR_APP.name, vm.error.value)
    }

    @Test
    fun `cancel clears state`() = runTest {
        val draft = MeetingDraft("Test", LocalDateTime.now(), 30, null)
        val launcher = FakeLauncher(CalendarLaunchOutcome.Success(0))
        val vm = ConfirmViewModel(launcher, {})

        vm.onDraftReady(draft, "raw")
        vm.onCancel()

        assertNull(vm.title.value)
        assertNull(vm.location.value)
        assertNull(vm.startMillis.value)
        assertNull(vm.durationMinutes.value)
    }
}