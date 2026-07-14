package com.pebblentn.app.rules

import java.util.regex.Pattern
import kotlin.math.roundToInt

/**
 * Extracts a distance in meters from free text (e.g. "In 500 m", "1.2 km", "0.3 mi"). Deterministic;
 * takes the first number+unit it finds. Decimal separators `.` and `,` are accepted.
 *
 * **The unit is mandatory.** A bare number in a navigation notification is far more often something
 * else: the ETA ("Arrive 23:41"), a road number ("A12"), or an exit number. Treating a unit-less
 * number as meters made every Google Maps step that carries no distance report "23 m", because the
 * engine matched the hour of the ETA in `subText` (real capture, 2026-07-13). Returns null when no
 * number *with a unit* is present — no distance is better than a wrong one.
 */
object DistanceParser {
    private val PATTERN: Pattern =
        Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*(km|mi|ft|m)\\b")

    fun parseMeters(text: String): Int? {
        val matcher = PATTERN.matcher(text)
        if (!matcher.find()) return null
        val number = matcher.group(1)?.replace(',', '.')?.toDoubleOrNull() ?: return null
        val meters = when (matcher.group(2)?.lowercase()) {
            "km" -> number * 1000.0
            "mi" -> number * 1609.344
            "ft" -> number * 0.3048
            else -> number // "m"
        }
        return meters.roundToInt().coerceAtLeast(0)
    }
}
