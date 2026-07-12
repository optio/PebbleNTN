package com.pebblentn.app.protocol

import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.core.NavigationInstruction
import com.pebblentn.app.core.NavigationSessionReducer
import com.pebblentn.app.core.NavigationState
import com.pebblentn.app.core.ReducerEffect
import com.pebblentn.app.core.ReducerEvent
import com.pebblentn.app.core.ReducerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeWatchTransportTest {

    @Test
    fun recordsLaunchesAndSends() = runTest {
        val transport = FakeWatchTransport()
        transport.launchApp()
        val result = transport.send(AppMessage.builder().putInt(Protocol.Keys.EVENT, 1).build())

        assertEquals(1, transport.launchCount)
        assertEquals(SendResult.SENT, result)
        assertEquals(1, transport.sent.size)
    }

    @Test
    fun scriptedSendResultsAreReturnedInOrderThenRepeatLast() = runTest {
        val transport = FakeWatchTransport(listOf(SendResult.FAILED, SendResult.SENT))
        val msg = AppMessage(emptyMap())
        assertEquals(SendResult.FAILED, transport.send(msg))
        assertEquals(SendResult.SENT, transport.send(msg))
        assertEquals(SendResult.SENT, transport.send(msg)) // repeats last
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun inboundMessagesAreObservable() = runTest {
        val transport = FakeWatchTransport()
        val ready = AppMessage.builder()
            .putInt(Protocol.Keys.EVENT, Protocol.Events.WATCH_READY)
            .putInt(Protocol.Keys.PROTOCOL_MAJOR, Protocol.MAJOR)
            .putInt(Protocol.Keys.PROTOCOL_MINOR, Protocol.MINOR)
            .build()

        val received = async { transport.inbound.first() }
        // Let the collector subscribe before emitting (replay=0 SharedFlow drops otherwise).
        runCurrent()
        transport.emitInbound(ready)
        assertEquals(Protocol.Events.WATCH_READY, received.await().intOrNull(Protocol.Keys.EVENT))
    }

    @Test
    fun endToEndReducerCodecTransportRoundTrip() = runTest {
        // Watch becomes ready -> phone receives an instruction -> reducer emits Launch + SendState
        // -> codec encodes -> transport records the NAVIGATION_UPDATE.
        val transport = FakeWatchTransport()
        var state = ReducerState()

        // Inbound WATCH_READY decoded into a reducer event.
        val readyMsg = AppMessage.builder()
            .putInt(Protocol.Keys.EVENT, Protocol.Events.WATCH_READY)
            .putInt(Protocol.Keys.PROTOCOL_MAJOR, Protocol.MAJOR)
            .putInt(Protocol.Keys.PROTOCOL_MINOR, Protocol.MINOR)
            .build()
        val readyEvent = ProtocolCodec.decodeInbound(readyMsg, atEpochSeconds = 0)!!
        state = dispatch(state, readyEvent, transport)

        // A parsed instruction flows through the reducer and out to the watch.
        val instr = ReducerEvent.InstructionReceived(
            NavigationInstruction(maneuver = Maneuver.RIGHT, distanceMeters = 200, primaryText = "Elm St"),
            atEpochSeconds = 1,
        )
        state = dispatch(state, instr, transport)

        assertEquals(1, transport.launchCount)
        val update = transport.sent.last()
        assertEquals(Protocol.Events.NAVIGATION_UPDATE, update.intOrNull(Protocol.Keys.EVENT))
        assertEquals(Maneuver.RIGHT.code, update.intOrNull(Protocol.Keys.MANEUVER))
        assertTrue(state.current is NavigationState.Navigating)
    }

    /** Minimal effect runner mirroring what the M2+ controller will do. */
    private suspend fun dispatch(
        state: ReducerState,
        event: ReducerEvent,
        transport: FakeWatchTransport,
    ): ReducerState {
        val result = NavigationSessionReducer.reduce(state, event)
        for (effect in result.effects) {
            when (effect) {
                ReducerEffect.LaunchWatchApp -> transport.launchApp()
                is ReducerEffect.SendState ->
                    transport.send(ProtocolCodec.encodeState(effect.state, effect.flags, "0.0.1"))
                is ReducerEffect.SendCompatibilityError ->
                    transport.send(ProtocolCodec.encodeCompatibilityError(effect.errorCode))
            }
        }
        return result.state
    }
}
