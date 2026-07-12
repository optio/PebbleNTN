package com.pebblentn.app.notification

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordingNotificationProcessorTest {

    @Test
    fun postedNotificationRecordsTimestamp() = runTest {
        val store = LastEligibleNotificationStore()
        val processor = RecordingNotificationProcessor(store)

        assertNull(store.lastEligibleAtMillis.value)
        processor.onEligiblePosted("com.google.android.apps.maps", postedAtMillis = 555)
        assertEquals(555L, store.lastEligibleAtMillis.value)
    }

    @Test
    fun laterNotificationUpdatesTimestamp() = runTest {
        val store = LastEligibleNotificationStore()
        val processor = RecordingNotificationProcessor(store)

        processor.onEligiblePosted("com.waze", 100)
        processor.onEligiblePosted("com.waze", 200)
        assertEquals(200L, store.lastEligibleAtMillis.value)
    }
}
