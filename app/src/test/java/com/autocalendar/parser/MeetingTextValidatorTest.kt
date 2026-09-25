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