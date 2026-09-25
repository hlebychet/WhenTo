package com.autocalendar.calendar

import android.provider.CalendarContract

data class EventToSave(
    val title: String,
    val beginMillis: Long,
    val endMillis: Long?,
    val location: String?,
)

object CalendarIntentBuilder {

    fun build(event: EventToSave): android.content.Intent {
        val intent = android.content.Intent(android.content.Intent.ACTION_INSERT)
        intent.data = android.net.Uri.parse("content://com.android.calendar/events")
        intent.putExtra(android.content.Intent.EXTRA_TITLE, event.title)
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.beginMillis)
        event.endMillis?.let { intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it) }
        event.location?.let { intent.putExtra("eventLocation", it) }
        return intent
    }
}