package com.pebblentn.app.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** #28: the dashboard's last-notification time survives a restart without hiding newer events. */
class LastEligibleNotificationStoreTest {

    @Test
    fun seedFillsAnEmptyStore() {
        val store = LastEligibleNotificationStore()
        assertNull(store.lastEligibleAtMillis.value)
        store.seed(1_000)
        assertEquals(1_000L, store.lastEligibleAtMillis.value)
    }

    @Test
    fun seedNeverReplacesANewerLiveValue() {
        val store = LastEligibleNotificationStore()
        store.record(5_000)
        store.seed(1_000)
        assertEquals(5_000L, store.lastEligibleAtMillis.value)
    }

    @Test
    fun aNewerSeedWins() {
        val store = LastEligibleNotificationStore()
        store.record(1_000)
        store.seed(2_000)
        assertEquals(2_000L, store.lastEligibleAtMillis.value)
    }
}
