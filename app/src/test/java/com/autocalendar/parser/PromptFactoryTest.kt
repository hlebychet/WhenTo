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
}