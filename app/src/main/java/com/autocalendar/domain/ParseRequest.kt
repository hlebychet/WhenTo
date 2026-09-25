package com.autocalendar.domain

import java.time.LocalDate

data class ParseRequest(
    val rawText: String,
    val today: LocalDate,
)