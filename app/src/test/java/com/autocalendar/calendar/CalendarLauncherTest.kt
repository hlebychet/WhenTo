package com.autocalendar.calendar

import android.content.Intent
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalendarLauncherTest {

    @Test
    fun `builds intent with all fields`() {
        val event = EventToSave(
            title = "Discuss mockup",
            beginMillis = 1_790_337_600_000L,
            endMillis = 1_790_337_600_000L + 30 * 60_000L,
            location = "Starbucks",
        )
        val intent = CalendarIntentBuilder.build(event)
        
        assertEquals(Intent.ACTION_INSERT, intent.action)
        assertEquals("content://com.android.calendar/events", intent.data.toString())
        assertEquals("Discuss mockup", intent.getStringExtra(Intent.EXTRA_TITLE))
        assertEquals(1_790_337_600_000L, intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, -1))
        assertEquals(1_790_337_600_000L + 30 * 60_000L, intent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, -1))
        assertEquals("Starbucks", intent.getStringExtra("eventLocation"))
    }

    @Test
    fun `builds intent without end time when null`() {
        val event = EventToSave(
            title = "Call",
            beginMillis = 1_790_337_600_000L,
            endMillis = null,
            location = null,
        )
        val intent = CalendarIntentBuilder.build(event)
        
        assertEquals(Intent.ACTION_INSERT, intent.action)
        assertEquals(1_790_337_600_000L, intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, -1))
        assertNull(intent.getStringExtra(CalendarContract.EXTRA_EVENT_END_TIME))
        assertNull(intent.getStringExtra("eventLocation"))
    }

    @Test
    fun `intent has correct action and data`() {
        val event = EventToSave("Test", 1_000_000_000_000L, null, null)
        val intent = CalendarIntentBuilder.build(event)
        assertNotNull(intent)
        assertEquals(Intent.ACTION_INSERT, intent.action)
        assertEquals("content://com.android.calendar/events", intent.data.toString())
    }
}