package com.pebblentn.app.update

/**
 * Compares dotted numeric app versions (e.g. "0.0.13" vs a GitHub tag "v0.0.14"). Tolerant of a
 * leading "v", of differing component counts ("1.2" vs "1.2.0"), and of a trailing pre-release
 * suffix ("1.2.0-rc1"), which is ignored. Non-numeric junk in a component is treated as 0 rather
 * than crashing — an unparseable remote tag must never make the app think it is out of date.
 *
 * [parseStrict] / [isNewerStrict] are the strict rule the watch also applies to the version the
 * other side reports (REQ-ANDROID-019, REQ-WATCH-023): no suffix, no junk, and an unparsable
 * version is never "newer". The tolerant pair stays for GitHub tags.
 */
object AppVersions {

    /** True when [latest] is a strictly higher version than [current]. */
    fun isNewer(current: String, latest: String): Boolean = compare(latest, current) > 0

    /**
     * The numeric parts of [version] under the strict rule, or null when it is unparsable: an
     * optional leading `v`/`V`, then one or more `.`-separated parts of 1 to 9 decimal digits each,
     * and nothing else (no suffix, sign, space or empty part). Same rule as the watch's
     * `version_parse` (watchapp/src/c/version_compare.c).
     */
    fun parseStrict(version: String?): List<Int>? {
        if (version == null) return null
        val core = if (version.startsWith('v') || version.startsWith('V')) version.substring(1) else version
        if (core.isEmpty()) return null
        val parts = core.split('.')
        if (parts.any { it.isEmpty() || it.length > MAX_PART_DIGITS || !it.all { c -> c in '0'..'9' } }) return null
        return parts.map(String::toInt)
    }

    /**
     * True only when both versions are parsable under [parseStrict] and [candidate] is higher than
     * [current], missing parts counting as 0 ("1.2" equals "1.2.0").
     */
    fun isNewerStrict(current: String?, candidate: String?): Boolean {
        val a = parseStrict(candidate) ?: return false
        val b = parseStrict(current) ?: return false
        return compareParts(a, b) > 0
    }

    /** Standard comparator: negative if [a] < [b], 0 if equal, positive if [a] > [b]. */
    fun compare(a: String, b: String): Int = compareParts(parts(a), parts(b))

    private fun compareParts(pa: List<Int>, pb: List<Int>): Int {
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    private fun parts(version: String): List<Int> {
        val core = version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
            .substringBefore('+')
        if (core.isEmpty()) return listOf(0)
        return core.split('.').map { segment ->
            segment.takeWhile(Char::isDigit).toIntOrNull() ?: 0
        }
    }

    /** 9 digits always fit an Int (and the watch's int32), so a strict part never overflows. */
    private const val MAX_PART_DIGITS = 9
}
