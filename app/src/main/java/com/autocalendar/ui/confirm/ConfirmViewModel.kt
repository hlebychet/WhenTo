package com.autocalendar.ui.confirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autocalendar.calendar.CalendarLauncher
import com.autocalendar.calendar.CalendarLaunchOutcome
import com.autocalendar.calendar.EventToSave
import com.autocalendar.calendar.LaunchFailureReason
import com.autocalendar.data.ParsedMeetingStore
import com.autocalendar.domain.MeetingDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

typealias TextValidator = (String) -> com.autocalendar.domain.ParseResult.Failure?

class ConfirmViewModel(
    private val launcher: CalendarLauncher,
    private val onEventSaved: (com.autocalendar.calendar.EventToSave) -> Unit,
    private val scope: CoroutineScope? = null,
) : ViewModel() {

    private val _scope: CoroutineScope = scope ?: viewModelScope

    private val _title = MutableStateFlow<String?>(null)
    val title: StateFlow<String?> = _title

    private val _location = MutableStateFlow<String?>(null)
    val location: StateFlow<String?> = _location

    private val _startMillis = MutableStateFlow<Long?>(null)
    val startMillis: StateFlow<Long?> = _startMillis

    private val _durationMinutes = MutableStateFlow<Int?>(null)
    val durationMinutes: StateFlow<Int?> = _durationMinutes

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun onDraftReady(draft: com.autocalendar.domain.MeetingDraft, rawText: String) {
        _title.value = draft.title
        _location.value = draft.location
        _startMillis.value = com.autocalendar.calendar.EventTimes.beginMillis(draft.startDateTime, java.time.ZoneId.systemDefault())
        _durationMinutes.value = draft.durationMinutes
    }

    fun onTitleChange(value: String) {
        _title.value = value
        _error.value = null
    }

    fun onLocationChange(value: String?) {
        _location.value = value
        _error.value = null
    }

    fun onCreateClick() {
        val title = _title.value ?: return
        val startMillis = _startMillis.value ?: return
        
        val event = com.autocalendar.calendar.EventToSave(
            title = title,
            beginMillis = startMillis,
            endMillis = _durationMinutes.value?.let { startMillis + it * 60_000L },
            location = _location.value,
        )

        val outcome = launcher.launch(event)
        when (outcome) {
            is com.autocalendar.calendar.CalendarLaunchOutcome.Success -> {
                onEventSaved(event)
                clear()
            }
            is com.autocalendar.calendar.CalendarLaunchOutcome.Failure -> {
                _error.value = outcome.reason.name
            }
        }
    }

    fun onCancel() {
        clear()
    }

    private fun clear() {
        _title.value = null
        _location.value = null
        _startMillis.value = null
        _durationMinutes.value = null
        _error.value = null
    }
}