package com.autocalendar.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autocalendar.data.ParsedMeeting
import com.autocalendar.data.ParsedMeetingStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
    store: ParsedMeetingStore,
    private val onSelect: (ParsedMeeting) -> Unit,
) : ViewModel() {

    val items: StateFlow<List<ParsedMeeting>> =
        store.observeAll().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    fun select(item: ParsedMeeting) {
        onSelect(item)
    }
}
