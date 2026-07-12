package com.pebblentn.app.protocol

import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.core.NavigationInstruction
import com.pebblentn.app.core.NavigationState
import com.pebblentn.app.core.ReducerEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolCodecTest {

    private val appVersion = "0.0.1"

    @Test
    fun encodeNavigatingCarriesEventAndFields() {
        val state = NavigationState.Navigating(
            sessionId = 7,
            instruction = NavigationInstruction(
                maneuver = Maneuver.RIGHT,
                distanceMeters = 150,
                primaryText = "Main St",
                etaEpochSeconds = 1_800_000_000L,
            ),
            stateTimestampSeconds = 1_700_000_000L,
        )
        val msg = ProtocolCodec.encodeState(state, flags = 0b10, appVersion = appVersion)

        assertEquals(Protocol.Events.NAVIGATION_UPDATE, msg.intOrNull(Protocol.Keys.EVENT))
        assertEquals(Maneuver.RIGHT.code, msg.intOrNull(Protocol.Keys.MANEUVER))
        assertEquals(150, msg.intOrNull(Protocol.Keys.DISTANCE_METERS))
        assertEquals("Main St", msg.stringOrNull(Protocol.Keys.PRIMARY_TEXT))
        assertEquals(7, msg.intOrNull(Protocol.Keys.SESSION_ID))
        assertEquals(0b10, msg.intOrNull(Protocol.Keys.FLAGS))
        assertEquals(1_700_000_000, msg.intOrNull(Protocol.Keys.STATE_TIMESTAMP_SECONDS))
        assertEquals(Protocol.MAJOR, msg.intOrNull(Protocol.Keys.PROTOCOL_MAJOR))
        assertEquals(appVersion, msg.stringOrNull(Protocol.Keys.APP_VERSION))
    }

    @Test
    fun encodeNavigatingOmitsAbsentOptionalFields() {
        val state = NavigationState.Navigating(
            sessionId = 1,
            instruction = NavigationInstruction(maneuver = Maneuver.STRAIGHT),
            stateTimestampSeconds = 100,
        )
        val msg = ProtocolCodec.encodeState(state, flags = 0, appVersion = appVersion)
        assertNull(msg.intOrNull(Protocol.Keys.DISTANCE_METERS))
        assertNull(msg.stringOrNull(Protocol.Keys.PRIMARY_TEXT))
        assertNull(msg.stringOrNull(Protocol.Keys.SECONDARY_TEXT))
        assertNull(msg.intOrNull(Protocol.Keys.ETA_EPOCH_SECONDS))
    }

    @Test
    fun encodeStoppedCarriesSessionAndFlags() {
        val msg = ProtocolCodec.encodeState(
            NavigationState.Stopped(sessionId = 3),
            flags = Protocol.FlagMasks.EXIT_TO_WATCHFACE_ON_STOP_MASK,
            appVersion = appVersion,
        )
        assertEquals(Protocol.Events.NAVIGATION_STOPPED, msg.intOrNull(Protocol.Keys.EVENT))
        assertEquals(3, msg.intOrNull(Protocol.Keys.SESSION_ID))
        assertEquals(Protocol.FlagMasks.EXIT_TO_WATCHFACE_ON_STOP_MASK, msg.intOrNull(Protocol.Keys.FLAGS))
    }

    @Test
    fun encodeNoActiveNavigation() {
        val msg = ProtocolCodec.encodeState(NavigationState.NoActiveNavigation, flags = 0, appVersion = appVersion)
        assertEquals(Protocol.Events.NO_ACTIVE_NAVIGATION, msg.intOrNull(Protocol.Keys.EVENT))
    }

    @Test
    fun encodeCompatibilityError() {
        val msg = ProtocolCodec.encodeCompatibilityError(Protocol.ErrorCodes.INCOMPATIBLE_PROTOCOL_MAJOR)
        assertEquals(Protocol.Events.PHONE_COMPATIBILITY_ERROR, msg.intOrNull(Protocol.Keys.EVENT))
        assertEquals(Protocol.ErrorCodes.INCOMPATIBLE_PROTOCOL_MAJOR, msg.intOrNull(Protocol.Keys.ERROR_CODE))
    }

    @Test
    fun longTextIsTruncatedToLimit() {
        val long = "x".repeat(ProtocolCodec.MAX_TEXT_CHARS + 50)
        val state = NavigationState.Navigating(
            sessionId = 1,
            instruction = NavigationInstruction(maneuver = Maneuver.LEFT, primaryText = long),
            stateTimestampSeconds = 0,
        )
        val text = ProtocolCodec.encodeState(state, 0, appVersion).stringOrNull(Protocol.Keys.PRIMARY_TEXT)!!
        assertTrue(text.length <= ProtocolCodec.MAX_TEXT_CHARS)
    }

    @Test
    fun decodeWatchReady() {
        val msg = AppMessage.builder()
            .putInt(Protocol.Keys.EVENT, Protocol.Events.WATCH_READY)
            .putInt(Protocol.Keys.PROTOCOL_MAJOR, 1)
            .putInt(Protocol.Keys.PROTOCOL_MINOR, 4)
            .build()
        val event = ProtocolCodec.decodeInbound(msg, atEpochSeconds = 42)
        assertEquals(ReducerEvent.WatchReady(1, 4, 42), event)
    }

    @Test
    fun decodeRequestState() {
        val msg = AppMessage.builder()
            .putInt(Protocol.Keys.EVENT, Protocol.Events.WATCH_REQUEST_STATE)
            .build()
        assertEquals(ReducerEvent.WatchRequestedState(9), ProtocolCodec.decodeInbound(msg, 9))
    }

    @Test
    fun decodeUnknownEventIsNull() {
        val msg = AppMessage.builder().putInt(Protocol.Keys.EVENT, 999).build()
        assertNull(ProtocolCodec.decodeInbound(msg, 0))
        assertNull(ProtocolCodec.decodeInbound(AppMessage(emptyMap()), 0))
    }
}
