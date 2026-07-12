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
        assertEquals(300, DistanceParser.parseMeters("300")) // no unit -> meters
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
