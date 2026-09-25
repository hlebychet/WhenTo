package com.autocalendar.parser

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.Locale

object NextWeekdayDateCorrector {

    private val enNames = mapOf(
        "monday" to DayOfWeek.MONDAY, "mon" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY, "tue" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY, "wed" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY, "thu" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY, "fri" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY, "sat" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY, "sun" to DayOfWeek.SUNDAY,
    )

    private val ruPrefixes = mapOf(
        "пон" to DayOfWeek.MONDAY,
        "вто" to DayOfWeek.TUESDAY,
        "сре" to DayOfWeek.WEDNESDAY,
        "чет" to DayOfWeek.THURSDAY,
        "пят" to DayOfWeek.FRIDAY,
        "суб" to DayOfWeek.SATURDAY,
        "вос" to DayOfWeek.SUNDAY,
    )

    fun resolve(rawText: String, today: LocalDate): LocalDate? {
        val english = Regex("\\b(?:next\\s+week\\s+|next\\s+)([a-z]+)", RegexOption.IGNORE_CASE)
            .findAll(rawText)
            .mapNotNull { match -> enNames[match.groupValues[1].lowercase(Locale.ROOT)] }
            .toList()
        val weekdays = english + russianWeekdays(rawText.lowercase(Locale.ROOT))
        if (weekdays.size != 1) return null
        return mondayOfNextWeek(today).with(weekdays.single())
    }

    private fun russianWeekdays(text: String): List<DayOfWeek> {
        val marker = "следующ"
        val found = mutableListOf<DayOfWeek>()
        val tokenizer = Regex("[а-яё]+")
        var idx = 0
        while (true) {
            idx = text.indexOf(marker, idx)
            if (idx < 0) break
            var scanned = 0
            for (token in tokenizer.findAll(text.substring(idx)).map { it.value }) {
                if (token.startsWith("следующ")) continue
                if (scanned >= 3) break
                scanned++
                val day = ruPrefixes.entries.firstOrNull { token.startsWith(it.key) }?.value
                if (day != null) {
                    found.add(day)
                    break
                }
            }
            idx += marker.length
        }
        return found
    }

    private fun mondayOfNextWeek(today: LocalDate): LocalDate {
        val weekBasedYear = today.get(IsoFields.WEEK_BASED_YEAR)
        val firstWeekAnchor = LocalDate.of(weekBasedYear, 1, 4)
        val currentWeek = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return firstWeekAnchor.with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, (currentWeek + 1).toLong())
    }
}