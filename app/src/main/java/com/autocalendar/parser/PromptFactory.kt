package com.autocalendar.parser

import java.time.LocalDate

object PromptFactory {

    fun build(rawText: String, today: LocalDate): String =
        """
        Today's date is ${today} (YYYY-MM-DD).

        A user pasted one or more messenger messages that arrange a meeting.
        Extract the meeting described in this text and base any relative dates
        ("tomorrow", "on Thursday", "next Friday") on today's date above.

        The title must be short and descriptive. Output the schema fields only —
        leave any field empty or null when the text does not say it.

        Messages:
        $rawText
        """.trimIndent()
}