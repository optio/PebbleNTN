package com.pebblentn.app.ui.format

import androidx.annotation.StringRes
import com.pebblentn.app.R
import com.pebblentn.app.catalog.NavigationAppCatalog
import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.data.DebugDisposition
import com.pebblentn.app.data.DebugEventType
import com.pebblentn.app.rules.RuleLayer
import com.pebblentn.app.rules.RuleOutcome

/**
 * One place that turns internal identifiers into what users read (#28): maneuver enums, debug
 * dispositions and event types become string resources, and package names become app names. Every
 * screen goes through here, so the same thing is always called the same way.
 */
object DisplayLabels {

    @StringRes
    fun maneuver(maneuver: Maneuver): Int = when (maneuver) {
        Maneuver.UNKNOWN -> R.string.maneuver_unknown
        Maneuver.STRAIGHT -> R.string.maneuver_straight
        Maneuver.SLIGHT_LEFT -> R.string.maneuver_slight_left
        Maneuver.LEFT -> R.string.maneuver_left
        Maneuver.SHARP_LEFT -> R.string.maneuver_sharp_left
        Maneuver.SLIGHT_RIGHT -> R.string.maneuver_slight_right
        Maneuver.RIGHT -> R.string.maneuver_right
        Maneuver.SHARP_RIGHT -> R.string.maneuver_sharp_right
        Maneuver.UTURN_LEFT -> R.string.maneuver_uturn_left
        Maneuver.UTURN_RIGHT -> R.string.maneuver_uturn_right
        Maneuver.ROUNDABOUT -> R.string.maneuver_roundabout
        Maneuver.ARRIVE -> R.string.maneuver_arrive
        Maneuver.TRANSIT -> R.string.maneuver_transit
    }

    /** A debug event's disposition; an unknown (future) value falls back to a generic label. */
    @StringRes
    fun disposition(disposition: String): Int = when (disposition) {
        DebugDisposition.MATCHED -> R.string.disposition_matched
        DebugDisposition.CAPTURED_UNMATCHED -> R.string.disposition_unmatched
        DebugDisposition.CAPTURED_NON_MANEUVER -> R.string.disposition_non_maneuver
        else -> R.string.disposition_other
    }

    @StringRes
    fun eventType(type: DebugEventType): Int = when (type) {
        DebugEventType.POSTED -> R.string.event_type_posted
        DebugEventType.REMOVED -> R.string.event_type_removed
    }

    @StringRes
    fun ruleOutcome(outcome: RuleOutcome): Int = when (outcome) {
        RuleOutcome.MATCHED -> R.string.rule_outcome_matched
        RuleOutcome.CONDITIONS_FAILED -> R.string.rule_outcome_conditions_failed
        RuleOutcome.DISABLED -> R.string.rule_outcome_disabled
        RuleOutcome.SKIPPED_LOCALE -> R.string.rule_outcome_skipped_locale
        RuleOutcome.ERROR -> R.string.rule_outcome_error
    }

    @StringRes
    fun ruleLayer(layer: RuleLayer): Int = when (layer) {
        RuleLayer.USER -> R.string.rule_layer_user
        RuleLayer.DOWNLOADED -> R.string.rule_layer_downloaded
        RuleLayer.BUNDLED -> R.string.rule_layer_bundled
    }

    /** The catalog's display name for [packageName], or the package name itself when it is unknown. */
    fun appName(catalog: NavigationAppCatalog, packageName: String): String =
        catalog.entryForPackage(packageName)?.displayName ?: packageName
}
