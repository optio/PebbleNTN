package com.pebblentn.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class NavigationInstructionTest {

    @Test
    fun defaultsAreUnknownAndEmpty() {
        val instruction = NavigationInstruction()
        assertEquals(Maneuver.UNKNOWN, instruction.maneuver)
        assertNull(instruction.distanceMeters)
        assertNull(instruction.primaryText)
        assertNull(instruction.secondaryText)
        assertNull(instruction.etaEpochSeconds)
    }

    @Test
    fun zeroDistanceIsAllowed() {
        assertEquals(0, NavigationInstruction(distanceMeters = 0).distanceMeters)
    }

    @Test
    fun stopsRemainingDefaultsToAbsentAndAllowsZero() {
        assertNull(NavigationInstruction().stopsRemaining)
        assertEquals(0, NavigationInstruction(stopsRemaining = 0).stopsRemaining)
    }

    @Test
    fun negativeStopsRemainingIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            NavigationInstruction(stopsRemaining = -1)
        }
    }

    @Test
    fun negativeDistanceIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            NavigationInstruction(distanceMeters = -1)
        }
    }
}
