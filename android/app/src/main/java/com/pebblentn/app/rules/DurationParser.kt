package com.pebblentn.app.rules

import java.util.regex.Pattern

/**
 * Extracts a duration in seconds from free text (e.g. "5 min", "1 h 20 min", "45 s"). Sums any
 * hour/minute/second components found. Returns null when no recognizable component is present.
 */
object DurationParser {
    private val HOURS: Pattern = Pattern.compile("(?i)(\\d+)\\s*(?:h|hr|hrs|hour|hours)\\b")
    private val MINUTES: Pattern = Pattern.compile("(?i)(\\d+)\\s*(?:min|mins|minute|minutes)\\b")
    private val SECONDS: Pattern = Pattern.compile("(?i)(\\d+)\\s*(?:s|sec|secs|second|seconds)\\b")

    fun parseSeconds(text: String): Int? {
        val hours = firstInt(HOURS, text)
        val minutes = firstInt(MINUTES, text)
        val seconds = firstInt(SECONDS, text)
        if (hours == null && minutes == null && seconds == null) return null
        return (hours ?: 0) * 3600 + (minutes ?: 0) * 60 + (seconds ?: 0)
    }

    private fun firstInt(pattern: Pattern, text: String): Int? {
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(1)?.toIntOrNull() else null
    }
}
