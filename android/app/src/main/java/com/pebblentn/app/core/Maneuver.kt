package com.pebblentn.app.core

import com.pebblentn.app.protocol.Protocol
import kotlinx.serialization.Serializable

/**
 * Normalized turn maneuver.
 *
 * Each maneuver carries the [code] sent over the wire in the AppMessage `MANEUVER` key; the codes
 * come from the generated [Protocol.ManeuverCodes] so the phone and watch never disagree. The enum
 * name doubles as the stable token that rule outputs use (e.g. the `maneuver` extractor emitting
 * `"RIGHT"`), so the rule JSON and this type share one vocabulary.
 */
@Serializable
enum class Maneuver(val code: Int) {
    UNKNOWN(Protocol.ManeuverCodes.UNKNOWN),
    STRAIGHT(Protocol.ManeuverCodes.STRAIGHT),
    SLIGHT_LEFT(Protocol.ManeuverCodes.SLIGHT_LEFT),
    LEFT(Protocol.ManeuverCodes.LEFT),
    SHARP_LEFT(Protocol.ManeuverCodes.SHARP_LEFT),
    SLIGHT_RIGHT(Protocol.ManeuverCodes.SLIGHT_RIGHT),
    RIGHT(Protocol.ManeuverCodes.RIGHT),
    SHARP_RIGHT(Protocol.ManeuverCodes.SHARP_RIGHT),
    UTURN_LEFT(Protocol.ManeuverCodes.UTURN_LEFT),
    UTURN_RIGHT(Protocol.ManeuverCodes.UTURN_RIGHT),
    ROUNDABOUT(Protocol.ManeuverCodes.ROUNDABOUT),
    ARRIVE(Protocol.ManeuverCodes.ARRIVE),
    ;

    companion object {
        private val byCode: Map<Int, Maneuver> = entries.associateBy(Maneuver::code)
        private val byToken: Map<String, Maneuver> = entries.associateBy { it.name }

        /** Maneuver for a wire [code], or [UNKNOWN] if the code is not recognized. */
        fun fromCode(code: Int): Maneuver = byCode[code] ?: UNKNOWN

        /**
         * Maneuver for a rule-output [token] (case-insensitive, surrounding whitespace ignored),
         * or [UNKNOWN] if the token is null/blank/unrecognized. Never throws — extraction of an
         * unexpected token degrades to UNKNOWN rather than failing the whole state.
         */
        fun fromToken(token: String?): Maneuver {
            if (token.isNullOrBlank()) return UNKNOWN
            return byToken[token.trim().uppercase()] ?: UNKNOWN
        }
    }
}
