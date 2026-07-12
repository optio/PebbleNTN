package com.pebblentn.app.core

/**
 * Quantizes a raw distance-to-maneuver (meters) onto coarse steps.
 *
 * Rationale: raw navigation distances change every few meters. Sending every value would spam the
 * watch and (with maneuver-change vibration) buzz constantly. Quantizing collapses near-identical
 * updates so the reducer can deduplicate them (REQ-WATCH-008, REQ-WATCH-009). Steps widen with
 * distance because precision matters near a turn, not a kilometer out.
 */
object DistanceQuantizer {

    /** Quantize [meters] (must be non-negative) to the nearest step for its range. */
    fun quantize(meters: Int): Int {
        require(meters >= 0) { "meters must be non-negative, was $meters" }
        val step = when {
            meters < 100 -> 10
            meters < 500 -> 25
            meters < 1000 -> 50
            else -> 100
        }
        return roundToStep(meters, step)
    }

    private fun roundToStep(value: Int, step: Int): Int = ((value + step / 2) / step) * step
}
