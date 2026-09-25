package com.autocalendar.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autocalendar.domain.MeetingDraft
import com.autocalendar.domain.ParseFailureReason
import com.autocalendar.domain.ParseRequest
import com.autocalendar.domain.ParseResult
import com.autocalendar.parser.MeetingParser
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

typealias TextValidator = (String) -> ParseResult.Failure?

class MainViewModel(
    private val parser: MeetingParser,
    private val validator: TextValidator,
    private val onDraftReady: (MeetingDraft, String) -> Unit,
    private val scope: CoroutineScope? = null,
) : ViewModel() {

    private val _scope: CoroutineScope = scope ?: viewModelScope

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun onTextChange(value: String) {
        _text.value = value
        clearError()
    }

    fun parse() {
        if (_isLoading.value) return

        val rawText = _text.value
        val failure = validator(rawText)
        if (failure != null) {
            _error.value = failure.reason.name
            return
        }

        _isLoading.value = true
        clearError()

        _scope.launch {
            val request = ParseRequest(rawText, LocalDate.now())
            val result = parser.parse(request)
            _isLoading.value = false
            when (result) {
                is ParseResult.Success -> onDraftReady(result.meeting, rawText)
                is ParseResult.Failure -> _error.value = result.reason.name
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}