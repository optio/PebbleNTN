package com.pebblentn.app.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistanceDurationParserTest {

    @Test
    fun parsesMetersAndKilometers() {
        assertEquals(500, DistanceParser.parseMeters("In 500 m, turn right"))
        assertEquals(1200, DistanceParser.parseMeters("1.2 km"))
        assertEquals(1200, DistanceParser.parseMeters("1,2 km")) // European decimal comma
        assertEquals(500, DistanceParser.parseMeters("500m"))
    }

    @Test
    fun parsesImperialUnits() {
        assertEquals(1609, DistanceParser.parseMeters("1 mi"))
        assertEquals(30, DistanceParser.parseMeters("100 ft"))
    }

    @Test
    fun noNumberIsNull() {
        assertNull(DistanceParser.parseMeters("turn right"))
        assertNull(DistanceParser.parseMeters(""))
    }

    /**
     * A unit-less number is not a distance. Google Maps' every notification carries the ETA in
     * subText ("Arrive 23:41"), and rules extract distance from combinedText — so treating a bare
     * number as meters reported "23 m" on the watch for every step with no real distance
     * (real capture, 2026-07-13). Road and exit numbers are the same trap.
     */
    @Test
    fun bareNumberIsNotADistance() {
        assertNull(DistanceParser.parseMeters("Head east on Sample Avenue Arrive 23:41"))
        assertNull(DistanceParser.parseMeters("300")) // no unit -> not a distance
        assertNull(DistanceParser.parseMeters("A12 ring exit 7"))
        // A real distance elsewhere in the same text is still found.
        assertEquals(300, DistanceParser.parseMeters("Turn right onto Example Street 300 m Arrive 23:59"))
    }

    @Test
    fun parsesDurationComponents() {
        assertEquals(300, DurationParser.parseSeconds("5 min"))
        assertEquals(45, DurationParser.parseSeconds("45 s"))
        assertEquals(4800, DurationParser.parseSeconds("1 h 20 min"))
        assertEquals(3600, DurationParser.parseSeconds("1 hour"))
    }

    @Test
    fun noDurationIsNull() {
        assertNull(DurationParser.parseSeconds("arrive"))
        assertNull(DurationParser.parseSeconds(""))
    }
}
