package com.autocalendar.parser

import org.json.JSONObject

object PlainDetectedMeetingAdapter {

    fun fromJson(text: String): DetectedMeeting? {
        val cleaned = text.trim()
            .replaceFirst(Regex("(?i)^```[a-z]*"), "")
            .replaceFirst(Regex("```\\s*$"), "")
            .trim()
        val whole = parseObject(cleaned)
        val obj = whole ?: parseObject(firstJsonObject(cleaned) ?: return null) ?: return null
        return fromObject(obj)
    }

    private fun parseObject(text: String): JSONObject? = try {
        JSONObject(text)
    } catch (e: Exception) {
        null
    }

    private fun firstJsonObject(text: String): String? {
        var depth = 0
        var start = -1
        for (index in text.indices) {
            when (text[index]) {
                '{' -> {
                    if (depth == 0) start = index
                    depth++
                }
                '}' -> if (depth > 0) {
                    depth--
                    if (depth == 0) return text.substring(start, index + 1)
                }
            }
        }
        return null
    }

    private fun fromObject(obj: JSONObject): DetectedMeeting = DetectedMeeting(
        title = obj.optString("title"),
        date = obj.optString("date"),
        time = obj.optString("time"),
        durationMinutes = obj.optInt("durationMinutes").takeIf { it > 0 },
        location = sanitizeNullString(obj.optString("location")),
    )

    private fun sanitizeNullString(s: String?): String? =
        s?.takeIf { it.isNotEmpty() && it != "null" }
}
