package com.pebblentn.app.rules

import com.pebblentn.app.notification.NotificationSnapshot

/** Resolves a rule `field` name to a value from a [NotificationSnapshot]. Unknown fields → null. */
object SnapshotFields {
    fun resolve(snapshot: NotificationSnapshot, field: String): String? = when (field) {
        "packageName" -> snapshot.packageName
        "title" -> snapshot.title
        "text" -> snapshot.text
        "subText" -> snapshot.subText
        "bigText" -> snapshot.bigText
        "summaryText" -> snapshot.summaryText
        "infoText" -> snapshot.infoText
        "category" -> snapshot.category
        "channelId" -> snapshot.channelId
        "template" -> snapshot.template
        "combinedText" -> snapshot.combinedText
        else -> null
    }?.let(::normalizeSpaces)

    /**
     * Map every Unicode space separator to an ASCII space before conditions or extractors see it.
     *
     * Navigation apps format the distance with a non-breaking space between number and unit —
     * `"350 m"`, `"92 m"` — so it never line-breaks (real captures from Google Maps,
     * CoMaps and Organic Maps, 2026-07). Java's `\s` on the desktop JVM does NOT match U+00A0, so
     * `DistanceParser` and the CoMaps/Organic-Maps title regexes silently failed on the true input
     * even though Android ART's Unicode-aware `\s` happened to match it on device. Normalizing here
     * makes matching deterministic and identical on both, and keeps rules written with plain `\s`
     * and a normal space working. The stored snapshot is untouched, so diagnostics keep the raw text.
     */
    fun normalizeSpaces(value: String): String {
        if (value.none { it != ' ' && Character.getType(it) == Character.SPACE_SEPARATOR.toInt() }) {
            return value
        }
        return buildString(value.length) {
            for (ch in value) {
                append(if (ch != ' ' && Character.getType(ch) == Character.SPACE_SEPARATOR.toInt()) ' ' else ch)
            }
        }
    }
}
