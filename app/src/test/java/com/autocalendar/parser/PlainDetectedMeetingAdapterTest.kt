package com.autocalendar.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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