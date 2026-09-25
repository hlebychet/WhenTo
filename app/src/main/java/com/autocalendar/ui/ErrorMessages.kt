package com.autocalendar.ui

import com.autocalendar.calendar.LaunchFailureReason
import com.autocalendar.domain.ParseFailureReason

fun ParseFailureReason.userMessage(): String = when (this) {
    ParseFailureReason.EMPTY_TEXT -> "Message is empty"
    ParseFailureReason.TOO_SHORT_TEXT -> "Message is too short"
    ParseFailureReason.NANO_UNAVAILABLE -> "On-device AI is not available"
    ParseFailureReason.MISSING_DATE_OR_TIME -> "No meeting date or time found"
    ParseFailureReason.MODEL_PARSE_FAILED -> "Could not understand the message"
    ParseFailureReason.QUOTA_EXCEEDED -> "On-device AI is busy. Try again."
    ParseFailureReason.SYSTEM_CALENDAR_MISSING -> "No calendar app found"
}

fun LaunchFailureReason.userMessage(): String = when (this) {
    LaunchFailureReason.NO_CALENDAR_APP -> "No calendar app found"
    LaunchFailureReason.ACTIVITY_NOT_FOUND -> "Calendar app could not be opened"
}
