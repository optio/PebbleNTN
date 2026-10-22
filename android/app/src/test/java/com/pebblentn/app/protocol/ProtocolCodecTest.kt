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
        assertNull(msg.intOrNull(Protocol.Keys.STOPS_REMAINING))
    }

    @Test
    fun encodeTransitCarriesManeuverAndStopsRemaining() {
        // REQ-WATCH-018: protocol 1.1 adds the optional stopsRemaining key for TRANSIT states.
        val state = NavigationState.Navigating(
            sessionId = 2,
            instruction = NavigationInstruction(
                maneuver = Maneuver.TRANSIT,
                primaryText = "Example Station",
                secondaryText = "20:26",
                stopsRemaining = 5,
            ),
            stateTimestampSeconds = 100,
        )
        val msg = ProtocolCodec.encodeState(state, flags = 0, appVersion = appVersion)
        assertEquals(Protocol.ManeuverCodes.TRANSIT, msg.intOrNull(Protocol.Keys.MANEUVER))
        assertEquals(5, msg.intOrNull(Protocol.Keys.STOPS_REMAINING))
        assertNull("a ride has no turn distance", msg.intOrNull(Protocol.Keys.DISTANCE_METERS))
        // #94 (FR1.1): protocol 1.2 adds the update flag and LATEST_PHONE_VERSION.
        assertEquals(2, msg.intOrNull(Protocol.Keys.PROTOCOL_MINOR))
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

    // REQ-ANDROID-017 / #94 (FR2): the update flag and latest version ride on every state message.

    private val navigating = NavigationState.Navigating(
        sessionId = 4,
        instruction = NavigationInstruction(maneuver = Maneuver.LEFT, distanceMeters = 80),
        stateTimestampSeconds = 100,
    )
    private val allStates = listOf(navigating, NavigationState.Stopped(sessionId = 4), NavigationState.NoActiveNavigation)

    @Test
    fun updateSetsBit4AndLatestVersionOnEveryStateMessage() {
        val otherBits = Protocol.FlagMasks.EXIT_TO_WATCHFACE_ON_STOP_MASK or Protocol.FlagMasks.STATE_IS_STALE_MASK
        for (state in allStates) {
            val msg = ProtocolCodec.encodeState(state, flags = otherBits, appVersion = appVersion, latestPhoneVersion = "v0.0.71")
            val flags = msg.intOrNull(Protocol.Keys.FLAGS)!!
            assertEquals("$state: bit 4", Protocol.FlagMasks.PHONE_UPDATE_AVAILABLE_MASK, flags and Protocol.FlagMasks.PHONE_UPDATE_AVAILABLE_MASK)
            assertEquals("$state: other bits kept", otherBits, flags and Protocol.FlagMasks.PHONE_UPDATE_AVAILABLE_MASK.inv())
            assertEquals("$state: leading v removed", "0.0.71", msg.stringOrNull(Protocol.Keys.LATEST_PHONE_VERSION))
        }
        // No active navigation, whose flags the reducer always sets to 0, still carries the bit.
        val idle = ProtocolCodec.encodeState(NavigationState.NoActiveNavigation, flags = 0, appVersion = appVersion, latestPhoneVersion = "0.0.71")
        assertEquals(Protocol.FlagMasks.PHONE_UPDATE_AVAILABLE_MASK, idle.intOrNull(Protocol.Keys.FLAGS))
        assertEquals("0.0.71", idle.stringOrNull(Protocol.Keys.LATEST_PHONE_VERSION))
    }

    @Test
    fun noUpdateLeavesBit4ClearAndKey14Out() {
        for (state in allStates) {
            for (latest in listOf(null, "", "  ", "v")) {
                val msg = ProtocolCodec.encodeState(state, flags = 0b10, appVersion = appVersion, latestPhoneVersion = latest)
                assertEquals("$state / '$latest': flags as given", 0b10, msg.intOrNull(Protocol.Keys.FLAGS))
                assertNull("$state / '$latest': no key 14", msg.stringOrNull(Protocol.Keys.LATEST_PHONE_VERSION))
            }
        }
    }

    @Test
    fun compatibilityErrorHasNoUpdateKey() {
        val msg = ProtocolCodec.encodeCompatibilityError(Protocol.ErrorCodes.INCOMPATIBLE_PROTOCOL_MAJOR)
        assertNull(msg.stringOrNull(Protocol.Keys.LATEST_PHONE_VERSION))
        assertNull(msg.intOrNull(Protocol.Keys.FLAGS))
    }

    @Test
    fun longTextIsTruncatedToLimit() {
        val long = "x".repeat(ProtocolCodec.MAX_PRIMARY_BYTES + 50)
        val state = NavigationState.Navigating(
            sessionId = 1,
            instruction = NavigationInstruction(maneuver = Maneuver.LEFT, primaryText = long),
            stateTimestampSeconds = 0,
        )
        val text = ProtocolCodec.encodeState(state, 0, appVersion).stringOrNull(Protocol.Keys.PRIMARY_TEXT)!!
        assertTrue(text.toByteArray(Charsets.UTF_8).size <= ProtocolCodec.MAX_PRIMARY_BYTES)
    }

    @Test
    fun textIsFittedInUtf8BytesOnWholeCharacters() {
        // 64 characters but more than 64 bytes: the watch copies bytes and would split the last "ä" (#76).
        val road = "Bundesstraße 51 Richtung Köln-Mülheim über Bergisch Gladbach Süd"
        val fitted = ProtocolCodec.fitText(road, ProtocolCodec.MAX_PRIMARY_BYTES)!!
        assertTrue(fitted.toByteArray(Charsets.UTF_8).size <= ProtocolCodec.MAX_PRIMARY_BYTES)
        // Cut back to a word boundary, with "…" to show the cut.
        assertEquals("Bundesstraße 51 Richtung Köln-Mülheim über Bergisch…", fitted)
    }

    @Test
    fun fittingKeepsShortTextAndNeverSplitsASurrogatePair() {
        assertEquals("Torstraße", ProtocolCodec.fitText("Torstraße", ProtocolCodec.MAX_PRIMARY_BYTES))
        assertEquals(null, ProtocolCodec.fitText(null, ProtocolCodec.MAX_PRIMARY_BYTES))
        val emoji = "🚗".repeat(30) // 4 bytes each, no spaces
        val fitted = ProtocolCodec.fitText(emoji, ProtocolCodec.MAX_SECONDARY_BYTES)!!
        assertTrue(fitted.toByteArray(Charsets.UTF_8).size <= ProtocolCodec.MAX_SECONDARY_BYTES)
        assertEquals("🚗".repeat(5) + "…", fitted)
    }

    @Test
    fun secondaryTextUsesTheWatchsSmallerBuffer() {
        val state = NavigationState.Navigating(
            sessionId = 1,
            instruction = NavigationInstruction(maneuver = Maneuver.LEFT, secondaryText = "noch 12,4 km bis zum Ziel über die Autobahn"),
            stateTimestampSeconds = 0,
        )
        val text = ProtocolCodec.encodeState(state, 0, appVersion).stringOrNull(Protocol.Keys.SECONDARY_TEXT)!!
        assertTrue(text, text.toByteArray(Charsets.UTF_8).size <= ProtocolCodec.MAX_SECONDARY_BYTES)
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

    // REQ-ANDROID-019 (FR3.1): the watchapp version from WATCH_READY's key 11.

    @Test
    fun watchReadyWithKey11CarriesTheWatchVersion() {
        val msg = AppMessage.builder()
            .putInt(Protocol.Keys.EVENT, Protocol.Events.WATCH_READY)
            .putInt(Protocol.Keys.PROTOCOL_MAJOR, Protocol.MAJOR)
            .putInt(Protocol.Keys.PROTOCOL_MINOR, Protocol.MINOR)
            .putString(Protocol.Keys.APP_VERSION, "0.0.72")
            .build()
        assertEquals("0.0.72", ProtocolCodec.watchAppVersion(msg))
    }

    @Test
    fun watchReadyWithoutKey11HasNoWatchVersion() {
        val bare = AppMessage.builder().putInt(Protocol.Keys.EVENT, Protocol.Events.WATCH_READY).build()
        assertNull(ProtocolCodec.watchAppVersion(bare))
        val blank = AppMessage.builder()
            .putInt(Protocol.Keys.EVENT, Protocol.Events.WATCH_READY)
            .putString(Protocol.Keys.APP_VERSION, " ")
            .build()
        assertNull("a blank version is no version", ProtocolCodec.watchAppVersion(blank))
    }

    @Test
    fun otherEventsHaveNoWatchVersion() {
        val request = AppMessage.builder()
            .putInt(Protocol.Keys.EVENT, Protocol.Events.WATCH_REQUEST_STATE)
            .putString(Protocol.Keys.APP_VERSION, "0.0.72")
            .build()
        assertNull(ProtocolCodec.watchAppVersion(request))
        assertNull(ProtocolCodec.watchAppVersion(AppMessage(emptyMap())))
    }
}
