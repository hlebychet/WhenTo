package com.autocalendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ParsedMeetingEntity::class], version = 1, exportSchema = false)
abstract class AutoCalendarDatabase : RoomDatabase() {

    abstract fun parsedMeetingDao(): ParsedMeetingDao

    companion object {
        fun create(context: Context): AutoCalendarDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AutoCalendarDatabase::class.java,
                "autocalendar.db",
            ).build()
    }
}