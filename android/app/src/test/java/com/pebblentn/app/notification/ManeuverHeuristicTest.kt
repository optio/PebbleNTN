package com.pebblentn.app.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManeuverHeuristicTest {

    @Test
    fun etaClockCountsAsAManeuver() {
        // Real turn-by-turn notifications carry an arrival time; a plain road-name update does not.
        assertTrue(ManeuverHeuristic.looksLikeManeuver("Head toward Elm Street Arrive 09:01"))
        assertTrue(ManeuverHeuristic.looksLikeManeuver("Svolta a sinistra 14:28")) // Italian, via clock
    }

    @Test
    fun maneuverKeywordsCountAcrossLanguages() {
        assertTrue(ManeuverHeuristic.looksLikeManeuver("Turn right onto Main Street"))
        assertTrue(ManeuverHeuristic.looksLikeManeuver("Svolta a destra"))       // it
        assertTrue(ManeuverHeuristic.looksLikeManeuver("Tournez à gauche"))      // fr
        assertTrue(ManeuverHeuristic.looksLikeManeuver("Rechts abbiegen"))       // de
        assertTrue(ManeuverHeuristic.looksLikeManeuver("Sla linksaf"))           // nl
        assertTrue(ManeuverHeuristic.looksLikeManeuver("En la rotonda"))         // es
    }

    @Test
    fun roadAndPlaceNamesAreNotManeuvers() {
        // The noise that floods real capture logs: road numbers and plain place names.
        assertFalse(ManeuverHeuristic.looksLikeManeuver("A184"))
        assertFalse(ManeuverHeuristic.looksLikeManeuver("High Street"))
        assertFalse(ManeuverHeuristic.looksLikeManeuver("Newcastle upon Tyne"))
    }

    @Test
    fun emptyOrNullIsNotManeuver() {
        assertFalse(ManeuverHeuristic.looksLikeManeuver(null))
        assertFalse(ManeuverHeuristic.looksLikeManeuver(""))
        assertFalse(ManeuverHeuristic.looksLikeManeuver("   "))
    }
}
