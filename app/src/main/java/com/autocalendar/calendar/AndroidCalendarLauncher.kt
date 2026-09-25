package com.autocalendar.calendar

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class AndroidCalendarLauncher(private val context: Context) : CalendarLauncher {

    override fun launch(event: EventToSave): CalendarLaunchOutcome {
        val intent = CalendarIntentBuilder.build(event)

        val packageManager = context.packageManager
        val activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        if (activities.isEmpty()) {
            return CalendarLaunchOutcome.Failure(LaunchFailureReason.NO_CALENDAR_APP)
        }

        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return CalendarLaunchOutcome.Success(0)
        } catch (e: android.content.ActivityNotFoundException) {
            return CalendarLaunchOutcome.Failure(LaunchFailureReason.ACTIVITY_NOT_FOUND)
        }
    }
}
