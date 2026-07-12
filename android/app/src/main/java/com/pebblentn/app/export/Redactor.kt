package com.pebblentn.app.export

import com.pebblentn.app.notification.NotificationSnapshot

/**
 * Structurally redacts notification text for privacy-safe diagnostic export (spec/300-data
 * ExportFormat.md). Digits, units and punctuation are preserved, and a small whitelist of maneuver
 * keywords is kept so a rule maintainer can still see the structure; every other word (road names,
 * destination labels, free-form text) is replaced with a fixed placeholder.
 */
class Redactor(private val placeholder: String = PLACEHOLDER) {

    private val word = Regex("\\p{L}+")

    fun redact(snapshot: NotificationSnapshot): NotificationSnapshot = snapshot.copy(
        title = redactText(snapshot.title),
        text = redactText(snapshot.text),
        subText = redactText(snapshot.subText),
        bigText = redactText(snapshot.bigText),
        summaryText = redactText(snapshot.summaryText),
        infoText = redactText(snapshot.infoText),
    )

    fun redactText(value: String?): String? {
        if (value == null) return null
        return word.replace(value) { match ->
            if (match.value.lowercase() in KEEP) match.value else placeholder
        }
    }

    companion object {
        const val PLACEHOLDER = "▮" // ▮

        /** Maneuver/structural keywords kept in privacy-safe exports. */
        val KEEP: Set<String> = setOf(
            "turn", "left", "right", "slight", "slightly", "sharp", "keep", "roundabout",
            "exit", "continue", "straight", "uturn", "u", "merge", "head", "go",
            "north", "south", "east", "west", "arrive", "arriving", "destination",
            "onto", "on", "in", "at", "take", "the", "toward", "towards", "now",
            "km", "m", "mi", "ft", "min", "mins", "minute", "minutes", "sec", "secs", "s", "h", "hr",
            "st", "nd", "rd", "th",
        )
    }
}
