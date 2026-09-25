package com.autocalendar.parser

import com.autocalendar.domain.ParseFailureReason
import com.autocalendar.domain.ParseRequest
import com.autocalendar.domain.ParseResult
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.delay

class GeminiNanoParser(
    private val model: GenerativeModel = Generation.getClient(),
    private val mapper: MeetingDraftMapper = MeetingDraftMapper(),
    private val maxQuotaRetries: Int = 3,
) : MeetingParser {

    override suspend fun parse(request: ParseRequest): ParseResult {
        val status = model.checkStatus()
        when (status) {
            0 -> return ParseResult.Failure(ParseFailureReason.NANO_UNAVAILABLE)
            1 -> model.download().collect { }
            2, 3 -> Unit
            else -> Unit
        }

        var attempt = 0
        while (true) {
            try {
                return runExtraction(request)
            } catch (e: Exception) {
                if (isQuotaError(e) && attempt < maxQuotaRetries) {
                    attempt++
                    delay(1_000L * attempt)
                    continue
                }
                return ParseResult.Failure(ParseFailureReason.QUOTA_EXCEEDED)
            }
        }
    }

    private suspend fun runExtraction(request: ParseRequest): ParseResult {
        val prompt = PromptFactory.build(request.rawText, request.today)

        val detected = plainJsonExtraction(request)

        if (detected == null) {
            return ParseResult.Failure(ParseFailureReason.MODEL_PARSE_FAILED)
        }

        val draft = mapper.fromDetected(detected)
            ?: return ParseResult.Failure(ParseFailureReason.MISSING_DATE_OR_TIME)

        return ParseResult.Success(draft)
    }

    private suspend fun plainJsonExtraction(request: ParseRequest): DetectedMeeting? {
        val prompt = PromptFactory.build(request.rawText, request.today)
        val response = model.generateContent(prompt) ?: return null
        // Try multiple ways to extract text from response
        val text = when (response) {
            is String -> response
            else -> try {
                // Try to get text via reflection or toString
                response.javaClass.getMethod("text").invoke(response) as? String
                    ?: response.javaClass.getMethod("toString").invoke(response) as? String
                    ?: return null
            } catch (e: Exception) {
                return null
            }
        }
        return PlainDetectedMeetingAdapter.fromJson(text)
    }

    private fun isQuotaError(e: Exception): Boolean {
        val msg = e.message ?: return false
        return msg.contains("BUSY", ignoreCase = true) ||
            msg.contains("QUOTA", ignoreCase = true) ||
            msg.contains("BATTERY", ignoreCase = true)
    }
}