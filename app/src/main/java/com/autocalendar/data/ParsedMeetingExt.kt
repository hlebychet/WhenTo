package com.autocalendar.data

import com.autocalendar.domain.MeetingDraft
import java.time.Instant
import java.time.ZoneId

fun ParsedMeeting.toDraft(zone: ZoneId = ZoneId.systemDefault()): MeetingDraft =
    MeetingDraft(
        title = title,
        startDateTime = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDateTime(),
        durationMinutes = endMillis?.let { ((it - startMillis) / 60_000L).toInt() },
        location = location,
    )
