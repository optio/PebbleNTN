package com.pebblentn.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DistanceQuantizerTest {

    @Test
    fun roundsToTenBelowOneHundred() {
        assertEquals(0, DistanceQuantizer.quantize(4))
        assertEquals(10, DistanceQuantizer.quantize(5))
        assertEquals(100, DistanceQuantizer.quantize(99))
    }

    @Test
    fun roundsToTwentyFiveBelowFiveHundred() {
        assertEquals(100, DistanceQuantizer.quantize(103))
        assertEquals(100, DistanceQuantizer.quantize(108))
        assertEquals(125, DistanceQuantizer.quantize(113))
        assertEquals(475, DistanceQuantizer.quantize(480))
    }

    @Test
    fun roundsToFiftyBelowOneThousand() {
        assertEquals(500, DistanceQuantizer.quantize(510))
        assertEquals(950, DistanceQuantizer.quantize(970))
    }

    @Test
    fun roundsToHundredAtOrAboveOneThousand() {
        assertEquals(1000, DistanceQuantizer.quantize(1040))
        assertEquals(2500, DistanceQuantizer.quantize(2549))
    }

    @Test
    fun negativeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { DistanceQuantizer.quantize(-1) }
    }
}
