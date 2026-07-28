package com.pebblentn.app.notification

/**
 * Cheap, language-agnostic test for whether an unmatched notification plausibly carries a turn
 * instruction — as opposed to the many non-maneuver updates a navigation app posts (the current
 * road name/number, "driving mode" cards, etc.). Real captures show that road-name updates flood the
 * capture history and inflate the "share to help" prompt, so only maneuver-like captures should count
 * as a gap worth reporting.
 *
 * Two signals, either is enough:
 * - an ETA clock time (`HH:MM`): navigation apps put the arrival time on the actual turn-by-turn
 *   notification but not on a plain road-name update. This is language- and app-independent and is
 *   the strongest signal in real captures.
 * - a maneuver/direction keyword in one of the supported languages, as a backup for a maneuver
 *   notification that happens to lack an ETA.
 *
 * It errs toward inclusion: a rare false positive just counts one extra capture, whereas excluding a
 * real gap would hide a missing rule/language.
 */
object ManeuverHeuristic {

    private val clock = Regex("""\b\d{1,2}:\d{2}\b""")
    private val word = Regex("""\p{L}+""")

    // Core direction/turn/roundabout/u-turn/arrive/continue words across the supported languages.
    // Deliberately excludes units and function words (unlike the redaction keep-list), so a bare
    // road name does not look like a maneuver.
    private val MANEUVER_WORDS: Set<String> = setOf(
        // en
        "turn", "left", "right", "slight", "sharp", "keep", "roundabout", "uturn", "merge", "exit",
        "straight", "continue", "proceed", "head", "arrive", "arriving", "arrived", "destination",
        // it
        "destra", "sinistra", "svolta", "gira", "rotonda", "rotatoria", "inversione", "prosegui",
        "dritto", "arrivo", "arrivato", "destinazione",
        // fr
        "droite", "gauche", "tournez", "giratoire", "serrez", "continuez", "arrivé", "arrivée",
        // es
        "derecha", "izquierda", "rotonda", "glorieta", "recto", "vuelta", "sentido", "llegado", "destino",
        // de
        "rechts", "links", "abbiegen", "kreisverkehr", "wenden", "geradeaus", "halten", "ziel", "angekommen",
        // nl
        "rechtsaf", "linksaf", "rotonde", "keer", "aanhouden", "rechtdoor", "aangekomen", "bestemming",
    )

    fun looksLikeManeuver(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        if (clock.containsMatchIn(text)) return true
        return word.findAll(text.lowercase()).any { it.value in MANEUVER_WORDS }
    }
}
