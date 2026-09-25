package com.autocalendar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ParsedMeetingDao {

    @Insert
    suspend fun insert(entity: ParsedMeetingEntity): Long

    @Query("SELECT * FROM parsed_meetings ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ParsedMeetingEntity>>
}