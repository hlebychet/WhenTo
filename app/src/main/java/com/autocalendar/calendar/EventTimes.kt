package com.autocalendar.calendar

import java.time.LocalDateTime
import java.time.ZoneId

object EventTimes {

    fun beginMillis(start: LocalDateTime, zone: ZoneId): Long =
        start.atZone(zone).toInstant().toEpochMilli()

    fun endMillis(beginMillis: Long, durationMinutes: Int?): Long? =
        if (durationMinutes == null) null else beginMillis + durationMinutes * 60_000L
}