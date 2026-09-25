package com.autocalendar.parser

import org.json.JSONObject

object PlainDetectedMeetingAdapter {

    fun fromJson(text: String): DetectedMeeting? {
        val cleaned = text.trim()
            .replaceFirst(Regex("(?i)^```[a-z]*"), "")
            .replaceFirst(Regex("```\\s*$"), "")
            .trim()
        val whole = parseObject(cleaned)
        val obj = whole ?: firstParsableObject(cleaned) ?: return null
        return fromObject(obj)
    }

    private fun parseObject(text: String): JSONObject? = try {
        JSONObject(text)
    } catch (e: Exception) {
        null
    }

    private fun firstParsableObject(text: String): JSONObject? {
        var depth = 0
        var start = -1
        var inString = false
        var escaped = false
        for (index in text.indices) {
            val ch = text[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    ch == '\\' -> escaped = true
                    ch == '"' -> inString = false
                }
                continue
            }
            when (ch) {
                '"' -> if (depth > 0) inString = true
                '{' -> {
                    if (depth == 0) start = index
                    depth++
                }
                '}' -> if (depth > 0) {
                    depth--
                    if (depth == 0) {
                        val candidate = parseObject(text.substring(start, index + 1))
                        if (candidate != null) return candidate
                    }
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
