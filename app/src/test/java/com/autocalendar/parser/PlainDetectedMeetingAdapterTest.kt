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
    fun `parses json preceded by prose`() {
        val detected = PlainDetectedMeetingAdapter.fromJson(
            "Here is the extracted meeting:\n{\"title\":\"Design review\",\"date\":\"2026-10-01\",\"time\":\"09:30\",\"durationMinutes\":45,\"location\":\"Room 2\"}",
        )
        assertEquals("Design review", detected?.title)
        assertEquals("2026-10-01", detected?.date)
        assertEquals("09:30", detected?.time)
        assertEquals(45, detected?.durationMinutes)
        assertEquals("Room 2", detected?.location)
    }

    @Test
    fun `parses an uppercase json fence`() {
        val detected = PlainDetectedMeetingAdapter.fromJson(
            """```JSON {"title":"Sprint planning","date":"2026-10-02","time":"10:00","durationMinutes":60,"location":null}```""",
        )
        assertEquals("Sprint planning", detected?.title)
        assertEquals("2026-10-02", detected?.date)
        assertEquals(60, detected?.durationMinutes)
        assertNull(detected?.location)
    }

    @Test
    fun `plain object input parses unchanged`() {
        val detected = PlainDetectedMeetingAdapter.fromJson(
            """{"title":"1:1","date":"2026-09-30","time":"11:00","durationMinutes":25,"location":"HQ"}""",
        )
        assertEquals("1:1", detected?.title)
        assertEquals("2026-09-30", detected?.date)
        assertEquals("11:00", detected?.time)
        assertEquals(25, detected?.durationMinutes)
        assertEquals("HQ", detected?.location)
    }

    @Test
    fun `garbage returns null`() {
        assertNull(PlainDetectedMeetingAdapter.fromJson("not json at all"))
    }
}