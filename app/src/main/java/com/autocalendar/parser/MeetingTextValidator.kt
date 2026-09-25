package com.autocalendar.parser

import com.autocalendar.domain.ParseFailureReason
import com.autocalendar.domain.ParseResult

object MeetingTextValidator {

    private const val MIN_MEANINGFUL_LENGTH = 12

    fun validate(rawText: String): ParseResult.Failure? {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            return ParseResult.Failure(ParseFailureReason.EMPTY_TEXT)
        }
        if (trimmed.length < MIN_MEANINGFUL_LENGTH) {
            return ParseResult.Failure(ParseFailureReason.TOO_SHORT_TEXT)
        }
        return null
    }
}