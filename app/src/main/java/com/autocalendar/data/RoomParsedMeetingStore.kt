package com.autocalendar.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomParsedMeetingStore(private val dao: ParsedMeetingDao) : ParsedMeetingStore {

    override suspend fun add(meeting: NewParsedMeeting): Long =
        dao.insert(
            ParsedMeetingEntity(
                rawText = meeting.rawText,
                title = meeting.title,
                startMillis = meeting.startMillis,
                endMillis = meeting.endMillis,
                location = meeting.location,
            ),
        )

    override fun observeAll(): Flow<List<ParsedMeeting>> =
        dao.observeAll().map { entities ->
            entities.map {
                ParsedMeeting(
                    id = it.id,
                    rawText = it.rawText,
                    title = it.title,
                    startMillis = it.startMillis,
                    endMillis = it.endMillis,
                    location = it.location,
                    createdAt = it.createdAt,
                )
            }
        }
}