package com.autocalendar.parser

import org.json.JSONObject

object PlainDetectedMeetingAdapter {

    fun fromJson(text: String): DetectedMeeting? =
        try {
            val trimmed = text.trim()
                .removePrefix("```json")
                .removeSuffix("```")
                .trim()
            val obj = JSONObject(trimmed)
            DetectedMeeting(
                title = obj.optString("title"),
                date = obj.optString("date"),
                time = obj.optString("time"),
                durationMinutes = obj.optInt("durationMinutes").takeIf { it > 0 },
                location = sanitizeNullString(obj.optString("location")),
            )
        } catch (e: Exception) {
            null
        }

    private fun sanitizeNullString(s: String?): String? =
        s?.takeIf { it.isNotEmpty() && it != "null" }
}