package com.pebblentn.app.notification

import com.pebblentn.app.core.AppAllowlist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationDispatcherTest {

    private fun queue(scheduler: kotlinx.coroutines.test.TestCoroutineScheduler) =
        SerialProcessingQueue(CoroutineScope(UnconfinedTestDispatcher(scheduler)))

    /** Spy processor recording every call, to prove disabled packages are never processed. */
    private class SpyProcessor : NotificationProcessor {
        val posted = mutableListOf<Pair<String, Long>>()
        val removed = mutableListOf<String>()
        override suspend fun onEligiblePosted(packageName: String, postedAtMillis: Long) {
            posted += packageName to postedAtMillis
        }
        override suspend fun onEligibleRemoved(packageName: String) {
            removed += packageName
        }
    }

    private val allowlist = AppAllowlist { it == "com.google.android.apps.maps" }

    @Test
    fun disabledPackageIsNeverProcessed() = runTest {
        val spy = SpyProcessor()
        val dispatcher = NotificationDispatcher(allowlist, queue(testScheduler), spy)

        dispatcher.onPosted("com.evil.spyware", postedAtMillis = 100)
        dispatcher.onRemoved("com.evil.spyware")
        testScheduler.advanceUntilIdle()

        assertTrue("disabled posts must not be processed", spy.posted.isEmpty())
        assertTrue("disabled removals must not be processed", spy.removed.isEmpty())
    }

    @Test
    fun enabledPackageIsProcessedWithTimestamp() = runTest {
        val spy = SpyProcessor()
        val dispatcher = NotificationDispatcher(allowlist, queue(testScheduler), spy)

        dispatcher.onPosted("com.google.android.apps.maps", postedAtMillis = 1_234)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("com.google.android.apps.maps" to 1_234L), spy.posted)
    }

    @Test
    fun nullPackageIsIgnored() = runTest {
        val spy = SpyProcessor()
        val dispatcher = NotificationDispatcher(allowlist, queue(testScheduler), spy)

        dispatcher.onPosted(null, postedAtMillis = 1)
        dispatcher.onRemoved(null)
        testScheduler.advanceUntilIdle()

        assertTrue(spy.posted.isEmpty())
        assertTrue(spy.removed.isEmpty())
    }
}
