package com.pebblentn.app.notification.icon

import android.app.Notification
import android.content.Context
import android.os.SystemClock
import com.pebblentn.app.rules.LayeredRules
import timber.log.Timber

/**
 * Recognises the maneuver drawn in a notification's large icon (#74). Returns null when the package
 * has no rule that maps icons, in which case the icon is never read.
 */
fun interface LargeIconRecognizer {
    fun recognize(packageName: String, notification: Notification): IconMatch?
}

/**
 * The device implementation: candidates from the active rules ([IconCandidates]), references from
 * the navigation app itself ([AppIconReferences]), and [IconMatcher]. The icon is read only here,
 * transiently: nothing but the matched drawable's name and a short diagnostic leaves this class
 * (REQ-SEC-003).
 */
class RuleDrivenLargeIconRecognizer(
    private val context: Context,
    private val rules: () -> LayeredRules,
    private val references: AppIconReferences = AppIconReferences(context),
) : LargeIconRecognizer {

    override fun recognize(packageName: String, notification: Notification): IconMatch? {
        val candidates = IconCandidates.forPackage(rules(), packageName)
        if (candidates.isEmpty()) return null
        val start = SystemClock.elapsedRealtimeNanos()
        val icon = AppIconReferences.largeIconAlpha(context, notification) ?: return IconMatch.NoMatch("no large icon")
        val match = IconMatcher.match(icon, references.references(packageName, candidates))
        // The cost per notification (#74 acceptance); the first call per app version also renders the references.
        Timber.d("Large-icon match for %s in %d µs: %s", packageName, (SystemClock.elapsedRealtimeNanos() - start) / 1000, match.diagnostic)
        return match
    }
}
