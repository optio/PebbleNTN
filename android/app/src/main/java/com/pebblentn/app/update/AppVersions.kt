package com.pebblentn.app.update

/**
 * Compares dotted numeric app versions (e.g. "0.0.13" vs a GitHub tag "v0.0.14"). Tolerant of a
 * leading "v", of differing component counts ("1.2" vs "1.2.0"), and of a trailing pre-release
 * suffix ("1.2.0-rc1"), which is ignored. Non-numeric junk in a component is treated as 0 rather
 * than crashing — an unparseable remote tag must never make the app think it is out of date.
 */
object AppVersions {

    /** True when [latest] is a strictly higher version than [current]. */
    fun isNewer(current: String, latest: String): Boolean = compare(latest, current) > 0

    /** Standard comparator: negative if [a] < [b], 0 if equal, positive if [a] > [b]. */
    fun compare(a: String, b: String): Int {
        val pa = parts(a)
        val pb = parts(b)
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
}
