package com.pebblentn.app.pebble

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.core.NavigationInstruction
import com.pebblentn.app.data.WatchSettingsRepository
import com.pebblentn.app.protocol.AppMessage
import com.pebblentn.app.protocol.Protocol
import com.pebblentn.app.protocol.SendResult
import com.pebblentn.app.protocol.WatchTransport
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Android-emulator half of the "auto-launch off, user opens the watchapp by hand" check
 * (REQ-WATCH-005, REQ-ANDROID-009). The emulator has no Bluetooth, so — per the spec's emulator
 * limitation — the watch link is a recording transport, but everything else is real: the persisted
 * setting, the controller, and the PebbleKit listener callback that the Core Devices companion
 * forwards when the watchapp comes to the foreground.
 *
 * The state message the phone sends is logged under [LOG_TAG] as JSON so
 * `scripts/test-manual-open-e2e.sh` can inject the exact same message into the Pebble emulator.
 */
@RunWith(AndroidJUnit4::class)
class ManualOpenWithAutoLaunchOffTest {

    private class RecordingTransport : WatchTransport {
        override val inbound: Flow<AppMessage> = WatchInboundBus.messages
        val launchCount = AtomicInteger()
        val sent = CopyOnWriteArrayList<AppMessage>()

        override suspend fun launchApp() {
            launchCount.incrementAndGet()
        }

        override suspend fun send(message: AppMessage): SendResult {
            sent += message
            return SendResult.SENT
        }
    }

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val settings = WatchSettingsRepository(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var previousAutoLaunch = WatchSettingsRepository.DEFAULT_AUTO_LAUNCH

    @Before
    fun disableAutoLaunch() {
        previousAutoLaunch = settings.isAutoLaunchEnabled()
        settings.setAutoLaunch(false)
    }

    @After
    fun restore() {
        scope.cancel()
        settings.setAutoLaunch(previousAutoLaunch)
    }

    @Test
    fun watchappOpenedByHandReceivesStateWithoutAnyLaunch(): Unit = runBlocking {
        val transport = RecordingTransport()
        val controller = NavigationController(
            transport = transport,
            scope = scope,
            appVersion = "e2e",
            initialSettings = settings.settings.value,
        )
        controller.start()
        delay(200) // let the inbound collector subscribe to the (replayless) bus

        controller.onInstruction(
            NavigationInstruction(Maneuver.RIGHT, distanceMeters = 450, primaryText = "Rue de la Loi"),
        )
        delay(2_500) // past the 1.5 s window in which a launch would schedule autonomous READY

        assertEquals("auto-launch is off: the watchapp must not be launched", 0, transport.launchCount.get())
        assertTrue("nothing is sent while the watchapp is closed", transport.sent.isEmpty())

        // The user opens PebbleNTN from the watch's launcher; the companion reports it.
        WatchListenerService().onAppOpened(PebbleWatchTransport.PEBBLENTN_UUID, WatchIdentifier("emulator"))

        val update = withTimeoutOrNull(5_000) {
            var found: AppMessage? = null
            while (found == null) {
                found = transport.sent.firstOrNull {
                    it.intOrNull(Protocol.Keys.EVENT) == Protocol.Events.NAVIGATION_UPDATE
                }
                if (found == null) delay(50)
            }
            found
        }
        assertNotNull("opening the watchapp by hand must deliver the current state", update)
        assertEquals("still never launched", 0, transport.launchCount.get())

        Log.i(LOG_TAG, "$OUTBOUND_MARKER${toJson(update!!)}")
    }

    private fun toJson(message: AppMessage): JSONObject {
        val ints = JSONObject()
        val strings = JSONObject()
        for ((key, value) in message.fields) {
            when (value) {
                is AppMessage.Value.IntValue -> ints.put(key.toString(), value.value)
                is AppMessage.Value.StrValue -> strings.put(key.toString(), value.value)
            }
        }
        return JSONObject().put("int", ints).put("string", strings)
    }

    private companion object {
        const val LOG_TAG = "PebbleNtnE2E"
        const val OUTBOUND_MARKER = "OUTBOUND "
    }
}
