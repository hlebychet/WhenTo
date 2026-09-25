package com.autocalendar.parser

import com.autocalendar.domain.ParseRequest
import com.autocalendar.domain.ParseResult

interface MeetingParser {
    suspend fun parse(request: ParseRequest): ParseResult
}