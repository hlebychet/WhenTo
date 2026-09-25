package com.autocalendar.domain

sealed interface ParseResult {
    data class Success(val meeting: MeetingDraft) : ParseResult
    data class Failure(val reason: ParseFailureReason) : ParseResult
}

enum class ParseFailureReason {
    EMPTY_TEXT,
    TOO_SHORT_TEXT,
    NANO_UNAVAILABLE,
    MISSING_DATE_OR_TIME,
    MODEL_PARSE_FAILED,
    QUOTA_EXCEEDED,
    SYSTEM_CALENDAR_MISSING,
}