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