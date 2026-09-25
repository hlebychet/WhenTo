package com.autocalendar.ui.main

import com.autocalendar.domain.MeetingDraft
import com.autocalendar.domain.ParseFailureReason
import com.autocalendar.domain.ParseRequest
import com.autocalendar.domain.ParseResult
import com.autocalendar.parser.MeetingParser
import com.autocalendar.parser.MeetingTextValidator
import com.autocalendar.ui.userMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
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
        
        // Use the test scope as the ViewModel's scope
        val vm = MainViewModel(parser, { null }, { d, _ -> emitted = d }, this)

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
        val vm = MainViewModel(parser, MeetingTextValidator::validate, { _, _ -> }, this)

        vm.onTextChange("")
        vm.parse()
        advanceUntilIdle()

        assertEquals(0, parser.calls)
        assertEquals(ParseFailureReason.EMPTY_TEXT.userMessage(), vm.error.value)
    }

    @Test
    fun `no-meeting short text is rejected`() = runTest {
        val parser = FakeParser(ParseResult.Failure(ParseFailureReason.TOO_SHORT_TEXT))
        val vm = MainViewModel(parser, MeetingTextValidator::validate, { _, _ -> }, this)

        vm.onTextChange("ok")
        vm.parse()
        advanceUntilIdle()

        assertEquals(0, parser.calls)
        assertEquals(ParseFailureReason.TOO_SHORT_TEXT.userMessage(), vm.error.value)
    }

    @Test
    fun `second parse while in flight is ignored`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val parser = FakeParser(ParseResult.Failure(ParseFailureReason.QUOTA_EXCEEDED), gate)
        val vm = MainViewModel(parser, { null }, { _, _ -> }, this)

        vm.onTextChange("Let's meet on Thursday at 3pm to discuss the mockup.")
        vm.parse()
        vm.parse() // second tap while first is still running
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, parser.calls)
    }
}