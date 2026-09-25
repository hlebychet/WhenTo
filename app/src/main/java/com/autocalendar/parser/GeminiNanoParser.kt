package com.autocalendar.parser

import com.autocalendar.domain.ParseFailureReason
import com.autocalendar.domain.ParseRequest
import com.autocalendar.domain.ParseResult
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

class GeminiNanoParser(
    private val model: GenerativeModel = Generation.getClient(),
    private val mapper: MeetingDraftMapper = MeetingDraftMapper(),
    private val maxQuotaRetries: Int = 3,
) : MeetingParser {

    override suspend fun parse(request: ParseRequest): ParseResult {
        val status = model.checkStatus()
        when (status) {
            STATUS_UNAVAILABLE -> return ParseResult.Failure(ParseFailureReason.NANO_UNAVAILABLE)
            STATUS_NEEDS_DOWNLOAD -> model.download().collect { }
            STATUS_DOWNLOADED, STATUS_AVAILABLE -> Unit
            else -> Unit
        }

        var attempt = 0
        while (true) {
            try {
                return runExtraction(request)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
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
        val response = model.generateContent(prompt)
        val text = response.candidates.firstOrNull()?.text ?: return null
        return PlainDetectedMeetingAdapter.fromJson(text)
    }

    private fun isQuotaError(e: Exception): Boolean {
        val msg = e.message ?: return false
        return msg.contains("BUSY", ignoreCase = true) ||
            msg.contains("QUOTA", ignoreCase = true) ||
            msg.contains("BATTERY", ignoreCase = true)
    }

    private companion object {
        const val STATUS_UNAVAILABLE = 0
        const val STATUS_NEEDS_DOWNLOAD = 1
        const val STATUS_DOWNLOADED = 2
        const val STATUS_AVAILABLE = 3
    }
}
