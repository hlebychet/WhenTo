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

    private val enPattern = Regex(
        "\\b(?:${enNames.keys.joinToString("|")})\\b",
        RegexOption.IGNORE_CASE,
    )

    private val ruPrefixes = listOf(
        "понедельник" to DayOfWeek.MONDAY, "пон" to DayOfWeek.MONDAY,
        "вторник" to DayOfWeek.TUESDAY, "вто" to DayOfWeek.TUESDAY,
        "среда" to DayOfWeek.WEDNESDAY, "сре" to DayOfWeek.WEDNESDAY,
        "четверг" to DayOfWeek.THURSDAY, "чет" to DayOfWeek.THURSDAY,
        "пятница" to DayOfWeek.FRIDAY, "пят" to DayOfWeek.FRIDAY,
        "суббота" to DayOfWeek.SATURDAY, "суб" to DayOfWeek.SATURDAY,
        "воскресенье" to DayOfWeek.SUNDAY, "вос" to DayOfWeek.SUNDAY,
    )

    private val ruShort = mapOf(
        "пн" to DayOfWeek.MONDAY, "вт" to DayOfWeek.TUESDAY, "ср" to DayOfWeek.WEDNESDAY,
        "чт" to DayOfWeek.THURSDAY, "пт" to DayOfWeek.FRIDAY, "сб" to DayOfWeek.SATURDAY,
        "вс" to DayOfWeek.SUNDAY,
    )

    private val nextWeekEn = Regex("\\b(?:next\\s+week\\s+|next\\s+)([a-z]+)", RegexOption.IGNORE_CASE)
    private val ruMarker = "следующ"
    private val ruToken = Regex("[а-яё]+")

    private val isoDate = Regex("\\d{4}-\\d{2}-\\d{2}")
    private val numericDate = Regex("\\d{1,2}[./-]\\d{1,2}")
    private val ruMonthDay = Regex("(?:январ|феврал|март|апрел|ма[йя]|июн|июл|август|сентябр|октябр|ноябр|декабр)[а-яё]*\\s+\\d{1,2}", RegexOption.IGNORE_CASE)
    private val ruDayMonth = Regex("\\d{1,2}\\s+(?:января|февраля|марта|апреля|мая|июня|июля|августа|сентября|октября|ноября|декабря)", RegexOption.IGNORE_CASE)
    private val enMonthDay = Regex("\\b(?:january|february|march|april|may|june|july|august|september|october|november|december)\\s+\\d{1,2}\\b", RegexOption.IGNORE_CASE)

    fun resolve(rawText: String, today: LocalDate): LocalDate? {
        val lower = rawText.lowercase(Locale.ROOT)
        val nextQualified = nextQualifiedWeekdays(lower)
        val allWeekdays = allWeekdays(lower)
        val distinct = (allWeekdays + nextQualified).toSet()
        if (distinct.size != 1) return null
        val day = distinct.single()
        if (hasExplicitDate(rawText)) return null
        return if (day in nextQualified) {
            mondayOfNextWeek(today).with(day)
        } else {
            nearestUpcoming(today, day)
        }
    }

    private fun nextQualifiedWeekdays(lower: String): List<DayOfWeek> {
        val english = nextWeekEn
            .findAll(lower)
            .mapNotNull { match -> enNames[match.groupValues[1].lowercase(Locale.ROOT)] }
            .toList()
        return english + russianNextWeekdays(lower)
    }

    private fun allWeekdays(lower: String): List<DayOfWeek> {
        val english = enPattern
            .findAll(lower)
            .map { enNames.getValue(it.value.lowercase(Locale.ROOT)) }
            .toList()
        val russian = ruToken.findAll(lower)
            .map { it.value }
            .mapNotNull(::ruWeekday)
            .toList()
        return english + russian
    }

    private fun russianNextWeekdays(text: String): List<DayOfWeek> {
        val found = mutableListOf<DayOfWeek>()
        var idx = 0
        while (true) {
            idx = text.indexOf(ruMarker, idx)
            if (idx < 0) break
            var scanned = 0
            for (token in ruToken.findAll(text.substring(idx)).map { it.value }) {
                if (token.startsWith(ruMarker)) continue
                if (scanned >= 3) break
                scanned++
                val day = ruWeekday(token)
                if (day != null) {
                    found.add(day)
                    break
                }
            }
            idx += ruMarker.length
        }
        return found
    }

    private fun ruWeekday(token: String): DayOfWeek? {
        ruShort[token]?.let { return it }
        return ruPrefixes.firstOrNull { token.startsWith(it.first) }?.second
    }

    private fun hasExplicitDate(rawText: String): Boolean {
        if (isoDate.containsMatchIn(rawText)) return true
        if (numericDate.containsMatchIn(rawText)) return true
        if (ruMonthDay.containsMatchIn(rawText)) return true
        if (ruDayMonth.containsMatchIn(rawText)) return true
        return enMonthDay.containsMatchIn(rawText)
    }

    private fun nearestUpcoming(today: LocalDate, day: DayOfWeek): LocalDate {
        var result = today
        while (result.dayOfWeek != day) result = result.plusDays(1)
        return result
    }

    private fun mondayOfNextWeek(today: LocalDate): LocalDate {
        val weekBasedYear = today.get(IsoFields.WEEK_BASED_YEAR)
        val firstWeekAnchor = LocalDate.of(weekBasedYear, 1, 4)
        val currentWeek = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return firstWeekAnchor.with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, (currentWeek + 1).toLong())
    }
}