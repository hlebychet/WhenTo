package com.autocalendar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parsed_meetings")
data class ParsedMeetingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawText: String,
    val title: String,
    val startMillis: Long,
    val endMillis: Long?,
    val location: String?,
    val createdAt: Long = System.currentTimeMillis(),
)