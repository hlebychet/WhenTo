package com.autocalendar

import android.content.Context
import com.autocalendar.calendar.AndroidCalendarLauncher
import com.autocalendar.calendar.CalendarLauncher
import com.autocalendar.data.AutoCalendarDatabase
import com.autocalendar.data.ParsedMeetingStore
import com.autocalendar.data.RoomParsedMeetingStore
import com.autocalendar.domain.MeetingDraft
import com.autocalendar.parser.GeminiNanoParser
import com.autocalendar.parser.MeetingParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow

data class ConfirmRequest(
    val draft: MeetingDraft,
    val rawText: String,
)

class AppContainer(context: Context) {

    private val database = AutoCalendarDatabase.create(context)

    val parser: MeetingParser = GeminiNanoParser()
    val store: ParsedMeetingStore = RoomParsedMeetingStore(database.parsedMeetingDao())
    val launcher: CalendarLauncher = AndroidCalendarLauncher(context)
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val pendingSharedText = MutableStateFlow<String?>(null)
    val confirmRequest = MutableStateFlow<ConfirmRequest?>(null)
}
