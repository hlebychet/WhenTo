package com.autocalendar.calendar

sealed interface CalendarLaunchOutcome {
    data class Success(val requestCode: Int) : CalendarLaunchOutcome
    data class Failure(val reason: LaunchFailureReason) : CalendarLaunchOutcome
}

enum class LaunchFailureReason {
    NO_CALENDAR_APP,
    ACTIVITY_NOT_FOUND,
}

interface CalendarLauncher {
    fun launch(event: EventToSave): CalendarLaunchOutcome
}