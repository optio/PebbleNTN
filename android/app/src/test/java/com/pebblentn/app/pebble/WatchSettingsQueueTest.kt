package com.pebblentn.app.pebble

import com.pebblentn.app.core.WatchSetting
import com.pebblentn.app.data.InMemoryWatchSettingsStore
import com.pebblentn.app.protocol.AppMessage
import com.pebblentn.app.protocol.FakeWatchTransport
import com.pebblentn.app.protocol.Protocol
import com.pebblentn.app.protocol.SendResult
import com.pebblentn.app.protocol.WatchMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phone edits of the watch settings wait while the watchapp is closed and go out once it opens
 * (REQ-ANDROID-021, #102 FR4), and never through a send that could relaunch it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WatchSettingsQueueTest {

    private fun controller(transport: FakeWatchTransport, scope: CoroutineScope, store: InMemoryWatchSettingsStore) =
        NavigationController(transport, scope, appVersion = "0.0.77", clock = { 0 }, watchSettings = store)

    private fun settingsSent(transport: FakeWatchTransport): List<AppMessage> =
        transport.sent.filter { it.intOrNull(Protocol.Keys.EVENT) == Protocol.Events.SETTINGS }

    /** A controller that has seen the watchapp's settings at [revision] and then the watchapp close. */
    private suspend fun TestScope.closedAfterSettings(
        transport: FakeWatchTransport,
        store: InMemoryWatchSettingsStore,
        revision: Int = 10,
    ): NavigationController {
        val controller = controller(transport, backgroundScope, store)
        controller.start()
        runCurrent()
        transport.emitInbound(WatchMessages.ready13(WatchMessages.settings(revision)))
        runCurrent()
        transport.emitClosed()
        runCurrent()
        return controller
    }

    @Test
    fun anEditWhileClosedIsQueuedAndSentOnceOnTheNextRealReady() = runTest {
        val transport = FakeWatchTransport()
        val store = InMemoryWatchSettingsStore()
        val controller = closedAfterSettings(transport, store)
        assertFalse(controller.watchappOpen.value)

        controller.onPhoneSettingEdit(WatchSetting.UNITS, 1)
        runCurrent()
        assertTrue("nothing goes to a closed watchapp", settingsSent(transport).isEmpty())
        assertEquals(setOf(WatchSetting.UNITS), store.state.value.pending)

        // The watchapp opens again with its unchanged settings: the queued edit is newer and goes out.
        transport.emitInbound(WatchMessages.ready13(WatchMessages.settings(revision = 10)))
        runCurrent()
        val sent = settingsSent(transport)
        assertEquals(1, sent.size)
        assertEquals(11, sent.single().intOrNull(Protocol.Keys.SETTINGS_REVISION))
        assertEquals(1, sent.single().intOrNull(Protocol.Keys.SETTING_UNITS))
        assertEquals("through the send that never relaunches", sent, transport.sentToOpenApp)
        assertEquals("no launch", 0, transport.launchCount)

        // The watch confirms: nothing is pending and nothing is sent again.
        transport.emitInbound(WatchMessages.settingsMessage(WatchMessages.settings(11, mapOf(WatchSetting.UNITS to 1))))
        runCurrent()
        assertTrue(store.state.value.pending.isEmpty())
        assertEquals("sent once", 1, settingsSent(transport).size)
    }

    @Test
    fun anEditWhileOpenGoesOutAtOnce() = runTest {
        val transport = FakeWatchTransport()
        val store = InMemoryWatchSettingsStore()
        val controller = controller(transport, backgroundScope, store)
        controller.start()
        runCurrent()
        transport.emitInbound(WatchMessages.ready13(WatchMessages.settings(revision = 3)))
        runCurrent()
        assertTrue(controller.watchappOpen.value)

        controller.onPhoneSettingEdit(WatchSetting.INVERT, 1)
        runCurrent()
        assertEquals(1, settingsSent(transport).single().intOrNull(Protocol.Keys.SETTING_INVERT))
    }

    @Test
    fun watchNewerSettingsReplaceTheQueuedValues() = runTest {
        val transport = FakeWatchTransport()
        val store = InMemoryWatchSettingsStore()
        val controller = closedAfterSettings(transport, store)
        controller.onPhoneSettingEdit(WatchSetting.UNITS, 1)
        runCurrent()

        // Meanwhile the watch was edited three times: its settings win and replace the queued edit.
        transport.emitInbound(WatchMessages.ready13(WatchMessages.settings(revision = 13, overrides = mapOf(WatchSetting.ACCENT to 4))))
        runCurrent()
        assertTrue(settingsSent(transport).isEmpty())
        assertTrue(store.state.value.pending.isEmpty())
        assertEquals(0, store.state.value.mirror!!.units)
        assertEquals(4, store.state.value.mirror!!.accent)
    }

    @Test
    fun aFailedSendKeepsTheEditForTheNextOpen() = runTest {
        // Navigation state still goes through; only the send to the open watchapp fails.
        val transport = object : FakeWatchTransport() {
            override suspend fun sendToOpenApp(message: AppMessage): SendResult {
                super.sendToOpenApp(message)
                return SendResult.FAILED
            }
        }
        val store = InMemoryWatchSettingsStore()
        val controller = controller(transport, backgroundScope, store)
        controller.start()
        runCurrent()
        transport.emitInbound(WatchMessages.ready13(WatchMessages.settings(revision = 3)))
        runCurrent()

        controller.onPhoneSettingEdit(WatchSetting.UNITS, 1)
        runCurrent()
        assertFalse("a failed send means it is not open", controller.watchappOpen.value)
        assertEquals(setOf(WatchSetting.UNITS), store.state.value.pending)
        controller.onPhoneSettingEdit(WatchSetting.INVERT, 1)
        runCurrent()
        assertEquals("no second try while closed", 1, settingsSent(transport).size)
    }

    @Test
    fun anOutdatedWatchappIsRecordedAndGetsNoSettings() = runTest {
        val transport = FakeWatchTransport()
        val store = InMemoryWatchSettingsStore()
        val controller = controller(transport, backgroundScope, store)
        controller.start()
        runCurrent()
        transport.emitInbound(WatchMessages.ready12())
        runCurrent()
        assertTrue(store.state.value.watchappOutdated)
        assertFalse(controller.watchappOpen.value)
        controller.onPhoneSettingEdit(WatchSetting.UNITS, 1)
        runCurrent()
        assertTrue(settingsSent(transport).isEmpty())
    }
}
