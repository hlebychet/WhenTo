package com.autocalendar.parser

import com.autocalendar.domain.MeetingDraft
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class MeetingDraftMapper {

    fun fromDetected(detected: DetectedMeeting): MeetingDraft? {
        val title = detected.title.trim()
        val date = parseDateOrNull(detected.date) ?: return null
        val time = parseTimeOrNull(detected.time) ?: return null
        if (title.isEmpty()) return null

        val startDateTime = LocalDateTime.of(date, time)
        val duration = detected.durationMinutes?.takeIf { it > 0 }

        return MeetingDraft(
            title = title,
            startDateTime = startDateTime,
            durationMinutes = duration,
            location = detected.location?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    private fun parseDateOrNull(text: String): LocalDate? =
        try {
            LocalDate.parse(text.trim())
        } catch (e: DateTimeParseException) {
            null
        }

    private fun parseTimeOrNull(text: String): java.time.LocalTime? {
        val regex = Regex("^([01]?\\d|2[0-3]):([0-5]\\d)$")
        val m = regex.matchEntire(text.trim()) ?: return null
        val hour = m.groupValues[1].toInt()
        val minute = m.groupValues[2].toInt()
        return java.time.LocalTime.of(hour, minute)
    }
}