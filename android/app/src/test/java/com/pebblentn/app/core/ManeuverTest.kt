package com.pebblentn.app.core

import com.pebblentn.app.protocol.Protocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ManeuverTest {

    @Test
    fun codesAreUniqueAcrossManeuvers() {
        val codes = Maneuver.entries.map { it.code }
        assertEquals("maneuver codes must be unique", codes.size, codes.toSet().size)
    }

    @Test
    fun fromCodeRoundTripsEveryManeuver() {
        for (maneuver in Maneuver.entries) {
            assertEquals(maneuver, Maneuver.fromCode(maneuver.code))
        }
    }

    @Test
    fun fromCodeUnknownCodeDegradesToUnknown() {
        assertEquals(Maneuver.UNKNOWN, Maneuver.fromCode(9999))
        assertEquals(Maneuver.UNKNOWN, Maneuver.fromCode(-1))
    }

    @Test
    fun fromTokenMatchesEnumNameCaseInsensitively() {
        assertEquals(Maneuver.RIGHT, Maneuver.fromToken("RIGHT"))
        assertEquals(Maneuver.RIGHT, Maneuver.fromToken("right"))
        assertEquals(Maneuver.SLIGHT_LEFT, Maneuver.fromToken("  Slight_Left  "))
    }

    @Test
    fun fromTokenNullOrBlankOrUnknownIsUnknown() {
        assertEquals(Maneuver.UNKNOWN, Maneuver.fromToken(null))
        assertEquals(Maneuver.UNKNOWN, Maneuver.fromToken(""))
        assertEquals(Maneuver.UNKNOWN, Maneuver.fromToken("   "))
        assertEquals(Maneuver.UNKNOWN, Maneuver.fromToken("banana"))
    }

    @Test
    fun codesMatchGeneratedProtocolConstants() {
        // Guards against the enum and the generated protocol drifting apart.
        assertEquals(Protocol.ManeuverCodes.STRAIGHT, Maneuver.STRAIGHT.code)
        assertEquals(Protocol.ManeuverCodes.RIGHT, Maneuver.RIGHT.code)
        assertEquals(Protocol.ManeuverCodes.ARRIVE, Maneuver.ARRIVE.code)
        assertEquals(Protocol.ManeuverCodes.UNKNOWN, Maneuver.UNKNOWN.code)
        assertNotEquals(Maneuver.LEFT.code, Maneuver.RIGHT.code)
    }
}
