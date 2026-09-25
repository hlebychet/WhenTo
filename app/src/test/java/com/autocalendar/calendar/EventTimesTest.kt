package com.autocalendar.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class EventTimesTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")

    @Test
    fun `begin millis matches local wall clock in zone`() {
        val start = LocalDateTime.of(2026, 9, 25, 15, 0)
        val expected = 1_790_337_600_000L // 2026-09-25 15:00 +03:00 = 12:00 UTC
        assertEquals(expected, EventTimes.beginMillis(start, zone))
    }

    @Test
    fun `end millis for 30 minutes`() {
        val begin = EventTimes.beginMillis(LocalDateTime.of(2026, 9, 25, 15, 0), zone)
        assertEquals(begin + 30 * 60_000L, EventTimes.endMillis(begin, 30))
    }

    @Test
    fun `end millis for 90 minutes`() {
        val begin = EventTimes.beginMillis(LocalDateTime.of(2026, 9, 25, 15, 0), zone)
        assertEquals(begin + 90 * 60_000L, EventTimes.endMillis(begin, 90))
    }

    @Test
    fun `end millis is null when duration absent`() {
        val begin = EventTimes.beginMillis(LocalDateTime.of(2026, 9, 25, 15, 0), zone)
        assertNull(EventTimes.endMillis(begin, null))
    }
}