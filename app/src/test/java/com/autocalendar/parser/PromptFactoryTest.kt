package com.autocalendar.parser

import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptFactoryTest {

    @Test
    fun `prompt embeds today's date`() {
        val prompt = PromptFactory.build("Let's meet tomorrow at 10", LocalDate.of(2026, 9, 24))
        assertTrue("prompt must embed today's date", prompt.contains("2026-09-24"))
        assertTrue(prompt.contains("Let's meet tomorrow at 10"))
    }

    @Test
    fun `prompt anchors relative dates explicitly to today`() {
        val prompt = PromptFactory.build("See you next Friday", LocalDate.of(2026, 9, 25))
        assertTrue(
            "prompt must instruct resolving relative dates from the given date",
            prompt.contains("reference date", ignoreCase = true) ||
                prompt.contains("based on today", ignoreCase = true),
        )
        assertTrue(
            "prompt must forbid using any other reference date",
            prompt.contains("only", ignoreCase = true) &&
                prompt.contains("today", ignoreCase = true),
        )
    }
}