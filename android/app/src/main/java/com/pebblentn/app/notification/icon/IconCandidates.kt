package com.pebblentn.app.notification.icon

import com.pebblentn.app.rules.LayeredRules
import com.pebblentn.app.rules.ManeuverMapExtractor

/**
 * Which of a navigation app's drawables to compare a large icon with, and what each one means: the
 * keys and values of the `maneuverMap` extractors that read the `iconDrawable` field in the
 * enabled rules for that package (#74). The rules are the single source of the drawable → maneuver
 * table, so the workbench and the regression fixtures see the same mapping as the device, and a
 * package without such a rule has its icon left unread.
 */
object IconCandidates {
    /** The snapshot field holding the matched drawable's name (see [com.pebblentn.app.rules.SnapshotFields]). */
    const val FIELD = "iconDrawable"

    /** Drawable name → maneuver for [packageName]; empty when no enabled rule maps icons. */
    fun forPackage(rules: LayeredRules, packageName: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for ((rule, _) in rules.inPrecedenceOrder()) {
            if (!rule.enabled || packageName !in rule.packageNames) continue
            val extractor = rule.output.maneuver as? ManeuverMapExtractor ?: continue
            if (extractor.field != FIELD) continue
            // Higher-precedence rules come first and win a disagreement about a drawable.
            for ((drawable, maneuver) in extractor.mapping) out.putIfAbsent(drawable, maneuver)
        }
        return out
    }
}
