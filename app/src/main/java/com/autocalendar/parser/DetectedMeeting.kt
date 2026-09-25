package com.autocalendar.parser

import com.google.mlkit.genai.schema.annotations.Generable
import com.google.mlkit.genai.schema.annotations.Guide

@Generable("A meeting extracted from a messenger message")
data class DetectedMeeting(
    @Guide(description = "Short descriptive meeting title")
    val title: String,

    @Guide(description = "Meeting date in YYYY-MM-DD format")
    val date: String,

    @Guide(description = "Meeting start time in 24h HH:mm format")
    val time: String,

    @Guide(description = "Meeting duration in minutes, or null if not mentioned",
        minimum = 1.0, maximum = 1440.0)
    val durationMinutes: Int?,

    @Guide(description = "Meeting location, or null if not mentioned")
    val location: String?,
)