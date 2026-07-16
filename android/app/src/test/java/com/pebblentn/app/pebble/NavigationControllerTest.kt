package com.pebblentn.app.pebble

import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.core.NavigationInstruction
import com.pebblentn.app.protocol.AppMessage
import com.pebblentn.app.protocol.FakeWatchTransport
import com.pebblentn.app.protocol.Protocol
import com.pebblentn.app.protocol.SendResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationControllerTest {

    private fun controller(transport: FakeWatchTransport, scope: kotlinx.coroutines.CoroutineScope) =
        NavigationController(transport, scope, appVersion = "0.0.1", clock = { 0 })

    private fun events(transport: FakeWatchTransport) =
        transport.sent.mapNotNull { it.intOrNull(Protocol.Keys.EVENT) }

    private fun readyMessage() = AppMessage.builder()
        .putInt(Protocol.Keys.EVENT, Protocol.Events.WATCH_READY)
        .putInt(Protocol.Keys.PROTOCOL_MAJOR, Protocol.MAJOR)
        .putInt(Protocol.Keys.PROTOCOL_MINOR, Protocol.MINOR)
        .build()

    @Test
    fun instructionBeforeReadyLaunchesOnceAndDefersSend() = runTest {
        val transport = FakeWatchTransport()
        val controller = controller(transport, backgroundScope)

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100))
        runCurrent()

        assertEquals(1, transport.launchCount)
        assertTrue("no state sent before READY", transport.sent.isEmpty())
    }

    @Test
    fun launchIsFollowedByAutonomousReadyWhenNoInboundHandshakeArrives() = runTest {
        // Mimic a companion app (e.g. the Core Devices Pebble app) that never forwards the watch's
        // WATCH_READY: no start(), no inbound READY. The controller must still send state on its own
        // after the settle delay, so the watch does not sit on "Connecting" forever.
        val transport = FakeWatchTransport()
        val controller = controller(transport, backgroundScope)

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100))
        runCurrent()
        assertEquals(1, transport.launchCount)
        assertTrue("nothing sent before the settle delay", transport.sent.isEmpty())

        testScheduler.advanceTimeBy(1_600) // pass the ~1.5s settle window
        runCurrent()

        assertTrue(
            "state is sent autonomously after launch, without an inbound READY",
            events(transport).contains(Protocol.Events.NAVIGATION_UPDATE),
        )
    }

    @Test
    fun watchReadySendsCurrentState() = runTest {
        val transport = FakeWatchTransport()
        val controller = controller(transport, backgroundScope)
        controller.start()
        runCurrent() // let the inbound collector subscribe before emitting

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100))
        transport.emitInbound(readyMessage())
        runCurrent()

        assertTrue(events(transport).contains(Protocol.Events.NAVIGATION_UPDATE))
    }

    @Test
    fun stopSendsStoppedWithExitFlag() = runTest {
        val transport = FakeWatchTransport()
        val controller = controller(transport, backgroundScope)
        controller.start()
        runCurrent() // let the inbound collector subscribe before emitting

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100))
        transport.emitInbound(readyMessage())
        runCurrent()
        controller.onNavigationStopped()
        runCurrent()

        val stopped = transport.sent.last()
        assertEquals(Protocol.Events.NAVIGATION_STOPPED, stopped.intOrNull(Protocol.Keys.EVENT))
        assertTrue(
            (stopped.intOrNull(Protocol.Keys.FLAGS) ?: 0) and Protocol.FlagMasks.EXIT_TO_WATCHFACE_ON_STOP_MASK != 0,
        )
    }

    @Test
    fun failedSendIsRetriedThenSucceeds() = runTest {
        // READY triggers a send; script the first send to fail, second to succeed.
        val transport = FakeWatchTransport(listOf(SendResult.FAILED, SendResult.SENT))
        val controller = controller(transport, backgroundScope)
        controller.start()
        runCurrent() // let the inbound collector subscribe before emitting

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100))
        transport.emitInbound(readyMessage())
        runCurrent() // process READY: first send fails and schedules a backoff retry
        testScheduler.advanceTimeBy(1_000) // advance the backoff window
        runCurrent() // run the retry send

        // The same state was sent twice (one failed attempt + one success).
        assertEquals(2, transport.sent.size)
    }

    @Test
    fun incompatibleWatchGetsCompatibilityError() = runTest {
        val transport = FakeWatchTransport()
        val controller = controller(transport, backgroundScope)
        controller.start()
        runCurrent() // let the inbound collector subscribe before emitting

        val incompatible = AppMessage.builder()
            .putInt(Protocol.Keys.EVENT, Protocol.Events.WATCH_READY)
            .putInt(Protocol.Keys.PROTOCOL_MAJOR, Protocol.MAJOR + 1)
            .putInt(Protocol.Keys.PROTOCOL_MINOR, Protocol.MINOR)
            .build()
        transport.emitInbound(incompatible)
        runCurrent()

        assertTrue(events(transport).contains(Protocol.Events.PHONE_COMPATIBILITY_ERROR))
    }

    /**
     * A transport whose inbound stream fails (e.g. the platform rejects the Pebble broadcast
     * receiver) must not take the app down: outbound navigation must keep working.
     */
    @Test
    fun failingInboundStreamDoesNotKillTheController() = runTest {
        val transport = object : FakeWatchTransport() {
            override val inbound = kotlinx.coroutines.flow.flow<AppMessage> {
                throw SecurityException("receiver rejected")
            }
        }
        val controller = controller(transport, backgroundScope)

        controller.start()
        runCurrent()

        controller.onInstruction(NavigationInstruction(Maneuver.RIGHT, distanceMeters = 100))
        runCurrent()

        assertEquals("watch app still launched", 1, transport.launchCount)
    }
}
