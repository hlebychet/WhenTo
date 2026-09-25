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