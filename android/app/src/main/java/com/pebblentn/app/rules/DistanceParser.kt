package com.pebblentn.app.rules

import java.util.regex.Pattern
import kotlin.math.roundToInt

/**
 * Extracts a distance in meters from free text (e.g. "In 500 m", "1.2 km", "0.3 mi", "800 metres",
 * "50 yd"). Deterministic; takes the first number+unit it finds. Decimal separators `.` and `,` are
 * accepted, and both abbreviated (m/km/ft/mi/yd) and spelled-out (meters/kilometres/feet/miles/
 * yards) units are recognized so rules for non-Google apps and other locales parse too.
 *
 * **The unit is mandatory.** A bare number in a navigation notification is far more often something
 * else: the ETA ("Arrive 23:41"), a road number ("A12"), or an exit number. Treating a unit-less
 * number as meters made every Google Maps step that carries no distance report "23 m", because the
 * engine matched the hour of the ETA in `subText` (real capture, 2026-07-13). Returns null when no
 * number *with a unit* is present — no distance is better than a wrong one. The unit must sit
 * immediately after the number, so a road name that merely contains a unit word does not match.
 *
 * Longer alternatives are listed before their prefixes (e.g. `metres` before `m`, `km` before `m`)
 * so a spelled-out unit is never truncated to a shorter one.
 */
object DistanceParser {
    private val PATTERN: Pattern =
        Pattern.compile(
            "(?i)(\\d+(?:[.,]\\d+)?)\\s*" +
                "(kilometres|kilometers|kilometre|kilometer|km" +
                "|miles|mile|mi|feet|foot|ft|yards|yard|yd" +
                "|metres|meters|metre|meter|m)\\b",
        )

    fun parseMeters(text: String): Int? {
        val matcher = PATTERN.matcher(text)
        if (!matcher.find()) return null
        val number = matcher.group(1)?.replace(',', '.')?.toDoubleOrNull() ?: return null
        val meters = when (matcher.group(2)?.lowercase()) {
            "km", "kilometer", "kilometers", "kilometre", "kilometres" -> number * 1000.0
            "mi", "mile", "miles" -> number * 1609.344
            "ft", "foot", "feet" -> number * 0.3048
            "yd", "yard", "yards" -> number * 0.9144
            else -> number // metre / metres / meter / meters / m
        }
        return meters.roundToInt().coerceAtLeast(0)
    }
}
