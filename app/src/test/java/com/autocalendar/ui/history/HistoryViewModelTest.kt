package com.autocalendar.ui.history

import com.autocalendar.data.ParsedMeeting
import com.autocalendar.data.ParsedMeetingStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryViewModelTest {

    private class FakeStore(items: List<ParsedMeeting>) : ParsedMeetingStore {
        private val flow = MutableStateFlow(items)

        override suspend fun add(meeting: com.autocalendar.data.NewParsedMeeting): Long = 0L
        override fun observeAll() = flow
    }

    private fun item(id: Long, title: String) = ParsedMeeting(
        id = id,
        rawText = "raw",
        title = title,
        startMillis = 1L,
        endMillis = null,
        location = null,
        createdAt = id,
    )

    @Test
    fun `items are exposed from the store`() = runTest {
        val store = FakeStore(listOf(item(2, "Second"), item(1, "First")))
        val vm = HistoryViewModel(store) {}

        assertEquals(listOf("Second", "First"), vm.items.first { it.isNotEmpty() }.map { it.title })
    }

    @Test
    fun `select re-emits the tapped item`() = runTest {
        val store = FakeStore(listOf(item(1, "First")))
        var selected: ParsedMeeting? = null
        val vm = HistoryViewModel(store) { selected = it }

        vm.select(item(1, "First"))

        assertEquals(1L, selected?.id)
    }
}
