package com.autocalendar.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ParsedMeetingStoreTest {

    private lateinit var db: AutoCalendarDatabase
    private lateinit var store: ParsedMeetingStore

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AutoCalendarDatabase::class.java,
        ).build()
        store = RoomParsedMeetingStore(db.parsedMeetingDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `add then observe returns the record`() = runTest {
        store.add(
            NewParsedMeeting(
                rawText = "Let's meet tomorrow at 10",
                title = "Discuss mockup",
                startMillis = 1_790_337_600_000L,
                endMillis = 1_790_337_600_000L + 30 * 60_000L,
                location = "Starbucks",
            ),
        )

        val items = store.observeAll().first()

        assertEquals(1, items.size)
        assertEquals("Discuss mockup", items[0].title)
        assertEquals("Starbucks", items[0].location)
        assertEquals(1_790_337_600_000L, items[0].startMillis)
    }

    @Test
    fun `records come back newest first`() = runTest {
        store.add(NewParsedMeeting("a", "First", 1L, null, null))
        store.add(NewParsedMeeting("b", "Second", 2L, null, null))

        val items = store.observeAll().first()

        assertEquals(listOf("Second", "First"), items.map { it.title })
    }
}