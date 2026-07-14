package com.pebblentn.app.notification

import com.pebblentn.app.core.AppAllowlist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationDispatcherTest {

    private fun queue(scheduler: TestCoroutineScheduler) =
        SerialProcessingQueue(CoroutineScope(UnconfinedTestDispatcher(scheduler)))

    private class SpyProcessor : NotificationProcessor {
        val posted = mutableListOf<PostedNotification>()
        val removed = mutableListOf<String>()
        override suspend fun onPosted(event: PostedNotification) { posted += event }
        override suspend fun onRemoved(packageName: String) { removed += packageName }
    }

    private val allowlist = AppAllowlist { it == "com.google.android.apps.maps" }

    private fun posted(pkg: String) = PostedNotification(
        snapshot = NotificationSnapshot(packageName = pkg, notificationId = 1, postTimeMillis = 10),
        notificationKey = "key",
        tag = null,
        receivedAtMillis = 20,
    )

    @Test
    fun disabledPackageIsNeverProcessedAndContentNeverBuilt() = runTest {
        val spy = SpyProcessor()
        val dispatcher = NotificationDispatcher(allowlist, queue(testScheduler), spy)
        var builderInvoked = false

        dispatcher.onPosted("com.evil.spyware") {
            builderInvoked = true // reading content would happen here
            posted("com.evil.spyware")
        }
        dispatcher.onRemoved("com.evil.spyware")
        testScheduler.advanceUntilIdle()

        assertFalse("content builder must not run for a disabled package", builderInvoked)
        assertTrue(spy.posted.isEmpty())
        assertTrue(spy.removed.isEmpty())
    }

    @Test
    fun enabledPackageIsProcessed() = runTest {
        val spy = SpyProcessor()
        val dispatcher = NotificationDispatcher(allowlist, queue(testScheduler), spy)

        dispatcher.onPosted("com.google.android.apps.maps") { posted("com.google.android.apps.maps") }
        testScheduler.advanceUntilIdle()

        assertEquals(1, spy.posted.size)
        assertEquals("com.google.android.apps.maps", spy.posted.single().snapshot.packageName)
    }

    @Test
    fun nullPackageIsIgnored() = runTest {
        val spy = SpyProcessor()
        val dispatcher = NotificationDispatcher(allowlist, queue(testScheduler), spy)

        dispatcher.onPosted(null) { posted("x") }
        dispatcher.onRemoved(null)
        testScheduler.advanceUntilIdle()

        assertTrue(spy.posted.isEmpty())
        assertTrue(spy.removed.isEmpty())
    }

    /**
     * The master switch (REQ-ANDROID-011) must gate *before* content is read — turning the app off
     * has to mean "reads nothing", not "reads it and throws it away", even for an allowlisted app.
     */
    @Test
    fun masterSwitchOffProcessesNothingAndNeverBuildsContent() = runTest {
        val spy = SpyProcessor()
        val dispatcher = NotificationDispatcher(allowlist, queue(testScheduler), spy, appEnabled = { false })
        var builderInvoked = false

        dispatcher.onPosted("com.google.android.apps.maps") {
            builderInvoked = true // reading content would happen here
            posted("com.google.android.apps.maps")
        }
        dispatcher.onRemoved("com.google.android.apps.maps")
        testScheduler.advanceUntilIdle()

        assertFalse("content must not be read while the app is disabled", builderInvoked)
        assertTrue(spy.posted.isEmpty())
        assertTrue(spy.removed.isEmpty())
    }

    @Test
    fun masterSwitchIsReReadPerEventSoTogglingTakesEffectImmediately() = runTest {
        val spy = SpyProcessor()
        var enabled = true
        val dispatcher = NotificationDispatcher(allowlist, queue(testScheduler), spy, appEnabled = { enabled })

        dispatcher.onPosted("com.google.android.apps.maps") { posted("com.google.android.apps.maps") }
        enabled = false
        dispatcher.onPosted("com.google.android.apps.maps") { posted("com.google.android.apps.maps") }
        testScheduler.advanceUntilIdle()

        assertEquals(1, spy.posted.size)
    }
}
