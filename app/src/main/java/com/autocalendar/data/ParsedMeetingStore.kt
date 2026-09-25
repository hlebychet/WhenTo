package com.autocalendar.data

import kotlinx.coroutines.flow.Flow

data class ParsedMeeting(
    val id: Long,
    val rawText: String,
    val title: String,
    val startMillis: Long,
    val endMillis: Long?,
    val location: String?,
    val createdAt: Long,
)

data class NewParsedMeeting(
    val rawText: String,
    val title: String,
    val startMillis: Long,
    val endMillis: Long?,
    val location: String?,
)

interface ParsedMeetingStore {
    suspend fun add(meeting: NewParsedMeeting): Long
    fun observeAll(): Flow<List<ParsedMeeting>>
}