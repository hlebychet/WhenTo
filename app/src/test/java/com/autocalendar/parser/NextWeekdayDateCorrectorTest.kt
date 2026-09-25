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
    fun `bare english weekday resolves to nearest upcoming occurrence`() {
        val todayFriday = LocalDate.of(2026, 9, 25)
        assertEquals(
            LocalDate.of(2026, 9, 25),
            NextWeekdayDateCorrector.resolve("Meeting on friday", todayFriday),
        )
        assertEquals(
            LocalDate.of(2026, 9, 26),
            NextWeekdayDateCorrector.resolve("Call on Saturday at 12", todayFriday),
        )
        assertEquals(
            LocalDate.of(2026, 10, 2),
            NextWeekdayDateCorrector.resolve("Let's sync on friday", LocalDate.of(2026, 9, 26)),
        )
    }

    @Test
    fun `bare russian weekday and abbreviations resolve to nearest upcoming occurrence`() {
        assertEquals(
            LocalDate.of(2026, 9, 26),
            NextWeekdayDateCorrector.resolve("Сб пока записал\n\nВ 15:00", LocalDate.of(2026, 9, 26)),
        )
        assertEquals(
            LocalDate.of(2026, 10, 1),
            NextWeekdayDateCorrector.resolve("Встреча в четверг в 15:00", LocalDate.of(2026, 9, 25)),
        )
        assertEquals(
            LocalDate.of(2026, 9, 29),
            NextWeekdayDateCorrector.resolve("созвон во вт в 12", LocalDate.of(2026, 9, 26)),
        )
        assertEquals(
            LocalDate.of(2026, 9, 27),
            NextWeekdayDateCorrector.resolve("напомни вс в 10 утра", LocalDate.of(2026, 9, 27)),
        )
    }

    @Test
    fun `explicit calendar date leaves bare weekday to the model`() {
        val todayFriday = LocalDate.of(2026, 9, 25)
        assertNull(NextWeekdayDateCorrector.resolve("встреча в субботу 03.10", todayFriday))
        assertNull(NextWeekdayDateCorrector.resolve("пятница 28 сентября в 11", todayFriday))
        assertNull(NextWeekdayDateCorrector.resolve("Meeting on Friday 2026-10-02", todayFriday))
    }

    @Test
    fun `multiple weekdays are ambiguous and not corrected`() {
        val todayFriday = LocalDate.of(2026, 9, 25)
        assertNull(NextWeekdayDateCorrector.resolve("Next Friday or next Monday", todayFriday))
        assertNull(NextWeekdayDateCorrector.resolve("работаю пн и ср", todayFriday))
    }

    @Test
    fun `text without a weekday leaves model result alone`() {
        val todayFriday = LocalDate.of(2026, 9, 25)
        assertNull(NextWeekdayDateCorrector.resolve("Hello, just chatting", todayFriday))
        assertNull(NextWeekdayDateCorrector.resolve("", todayFriday))
        assertNull(NextWeekdayDateCorrector.resolve("Следуй за мной", todayFriday))
    }
}