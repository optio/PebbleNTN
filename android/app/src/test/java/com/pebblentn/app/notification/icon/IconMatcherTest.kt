package com.pebblentn.app.notification.icon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The large-icon matcher (#74) on synthetic masks: simple shapes standing in for turn arrows, drawn
 * at the sizes real icons come in (84 / 126 / 168 px for one drawable at different densities).
 */
class IconMatcherTest {

    /** A [size]² mask with the pixels where [inside] (on a 0..1 grid) is true set opaque. */
    private fun mask(size: Int, inside: (Double, Double) -> Boolean) =
        AlphaImage(size, size, IntArray(size * size) { i -> if (inside((i % size + 0.5) / size, (i / size + 0.5) / size)) 255 else 0 })

    // Three clearly different shapes.
    private val leftBar = { x: Double, y: Double -> x < 0.35 && y > 0.2 }
    private val rightBar = { x: Double, y: Double -> x > 0.65 && y > 0.2 }
    private val topBar = { _: Double, y: Double -> y < 0.3 }

    private fun ref(name: String, maneuver: String, shape: (Double, Double) -> Boolean) =
        IconReference(name, maneuver, mask(168, shape).scaledTo(IconMatcher.SIZE))

    private val refs = listOf(ref("ic_left", "LEFT", leftBar), ref("ic_right", "RIGHT", rightBar), ref("ic_straight", "STRAIGHT", topBar))

    @Test
    fun matchesTheSameShapeExactly() {
        val m = IconMatcher.match(mask(168, rightBar), refs) as IconMatch.Matched
        assertEquals("ic_right", m.drawable)
        assertEquals("RIGHT", m.maneuver)
        assertEquals(0.0, m.distance, 0.01)
    }

    @Test
    fun matchesAcrossScreenDensities() {
        for (size in listOf(84, 126, 168, 224)) {
            val m = IconMatcher.match(mask(size, leftBar), refs)
            assertTrue("size $size: $m", m is IconMatch.Matched && m.maneuver == "LEFT")
        }
    }

    @Test
    fun ignoresColourBecauseOnlyAlphaIsCompared() {
        // A tinted icon has the same alpha mask; AlphaImage holds alpha only, so this is the same input.
        val m = IconMatcher.match(mask(126, topBar), refs) as IconMatch.Matched
        assertEquals("STRAIGHT", m.maneuver)
    }

    @Test
    fun anUnknownShapeIsNoMatch() {
        val ring = { x: Double, y: Double -> val d = (x - 0.5) * (x - 0.5) + (y - 0.5) * (y - 0.5); d in 0.1..0.2 }
        val m = IconMatcher.match(mask(126, ring), refs)
        assertTrue("$m", m is IconMatch.NoMatch && m.reason.contains("too far"))
    }

    @Test
    fun twoManeuversDrawnAlikeAreAmbiguous() {
        val nearlyLeft = { x: Double, y: Double -> x < 0.36 && y > 0.2 }
        val twins = listOf(ref("ic_left", "LEFT", leftBar), ref("ic_left_slight", "SLIGHT_LEFT", nearlyLeft))
        val m = IconMatcher.match(mask(126, leftBar), twins)
        assertTrue("$m", m is IconMatch.NoMatch && m.reason.startsWith("ambiguous"))
    }

    @Test
    fun identicalDrawablesForOneManeuverAreNotAmbiguous() {
        // Google Maps draws an on-ramp right and a turn right with the same arrow: same maneuver, no conflict.
        val withTwin = refs + ref("maneuver_on_ramp_normal_right", "RIGHT", rightBar)
        val m = IconMatcher.match(mask(126, rightBar), withTwin) as IconMatch.Matched
        assertEquals("RIGHT", m.maneuver)
    }

    @Test
    fun noReferencesOrABlankIconIsNoMatch() {
        assertEquals(IconMatch.NoMatch("no references"), IconMatcher.match(mask(126, leftBar), emptyList()))
        assertEquals(IconMatch.NoMatch("blank icon"), IconMatcher.match(mask(126) { _, _ -> false }, refs))
    }

    @Test
    fun scalingUpFillsEveryCell() {
        val small = mask(8, leftBar).scaledTo(IconMatcher.SIZE)
        assertEquals(IconMatcher.SIZE * IconMatcher.SIZE, small.alpha.size)
        assertTrue(small.alpha.first() == 0 || small.alpha.first() == 255)
        assertEquals("LEFT", (IconMatcher.match(mask(8, leftBar), refs) as IconMatch.Matched).maneuver)
    }

    @Test
    fun diagnosticNamesTheDrawableAndDistancesOnly() {
        val m = IconMatcher.match(mask(168, rightBar), refs)
        assertTrue(m.diagnostic, m.diagnostic.startsWith("ic_right 0.0 (next maneuver "))
    }
}
