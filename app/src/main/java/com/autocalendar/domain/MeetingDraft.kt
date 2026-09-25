package com.autocalendar.domain

import java.time.LocalDateTime

data class MeetingDraft(
    val title: String,
    val startDateTime: LocalDateTime,
    val durationMinutes: Int?,
    val location: String?,
)