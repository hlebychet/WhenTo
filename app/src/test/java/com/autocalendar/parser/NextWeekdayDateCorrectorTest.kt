package com.autocalendar.parser

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextWeekdayDateCorrectorTest {

    @Test
    fun `english next weekday resolves to that weekday of the following iso week`() {
        val todayFriday = LocalDate.of(2026, 9, 25)
        assertEquals(
            LocalDate.of(2026, 10, 2),
            NextWeekdayDateCorrector.resolve("Call about the apartment next Friday at 7 PM", todayFriday),
        )
        assertEquals(
            LocalDate.of(2026, 9, 28),
            NextWeekdayDateCorrector.resolve("Meeting next Monday", todayFriday),
        )
        assertEquals(
            LocalDate.of(2026, 9, 30),
            NextWeekdayDateCorrector.resolve("Lunch next Wednesday", todayFriday),
        )
    }

    @Test
    fun `russian next weekday resolves to that weekday of the following iso week`() {
        val todayFriday = LocalDate.of(2026, 9, 25)
        assertEquals(
            LocalDate.of(2026, 10, 2),
            NextWeekdayDateCorrector.resolve("Позвони маме в следующую пятницу в 19 часов", todayFriday),
        )
        assertEquals(
            LocalDate.of(2026, 10, 2),
            NextWeekdayDateCorrector.resolve("Встреча на следующей неделе в пятницу", todayFriday),
        )
    }

    @Test
    fun `case insensitive and optional week word`() {
        val todayFriday = LocalDate.of(2026, 10, 2)
        assertEquals(LocalDate.of(2026, 10, 9), NextWeekdayDateCorrector.resolve("Meeting NEXT WEEK friday", todayFriday))
        assertEquals(LocalDate.of(2026, 10, 5), NextWeekdayDateCorrector.resolve("next monday", todayFriday))
    }

    @Test
    fun `plain weekday without next marker is left to the model`() {
        val todayFriday = LocalDate.of(2026, 9, 25)
        assertNull(NextWeekdayDateCorrector.resolve("Meeting on friday", todayFriday))
        assertNull(NextWeekdayDateCorrector.resolve("Встреча в четверг в 15:00", todayFriday))
    }

    @Test
    fun `multiple next weekdays are ambiguous and not corrected`() {
        val todayFriday = LocalDate.of(2026, 9, 25)
        assertNull(NextWeekdayDateCorrector.resolve("Next Friday or next Monday", todayFriday))
    }

    @Test
    fun `text without a weekday leaves model result alone`() {
        val todayFriday = LocalDate.of(2026, 9, 25)
        assertNull(NextWeekdayDateCorrector.resolve("Hello, just chatting", todayFriday))
        assertNull(NextWeekdayDateCorrector.resolve("", todayFriday))
        assertNull(NextWeekdayDateCorrector.resolve("Следуй за мной", todayFriday))
    }
}