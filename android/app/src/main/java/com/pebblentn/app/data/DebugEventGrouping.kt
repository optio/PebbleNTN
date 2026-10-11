package com.pebblentn.app.data

import com.pebblentn.app.core.Hashing
import com.pebblentn.app.notification.NotificationSnapshot

/**
 * When two consecutive debug events are the same card (#86): Google Maps reposts its card every few
 * seconds with only the distance changed, so 500 events held a quarter of an hour of driving. The key
 * covers the card's layout and text with every number replaced, what PebbleNTN made of it (result,
 * icon match) and the versions involved; another road, rule, icon result or version starts a new
 * event. Hashed, so the stored key carries no notification text.
 */
object DebugEventGrouping {

    private val NUMBER = Regex("\\d+(?:[.,]\\d+)*")

    /** [value] with every number replaced by "N" ("2,6 km" and "900 m" both read "N km"/"N m"). */
    fun numberless(value: String?): String = value?.let { NUMBER.replace(it, "N") }.orEmpty()

    fun key(
        snapshot: NotificationSnapshot,
        disposition: String,
        matchedRuleId: String?,
        versions: EventVersions,
    ): String {
        val parts = listOf(
            snapshot.packageName,
            snapshot.channelId.orEmpty(),
            snapshot.category.orEmpty(),
            snapshot.template.orEmpty(),
            numberless(snapshot.title),
            numberless(snapshot.text),
            numberless(snapshot.subText),
            numberless(snapshot.bigText),
            numberless(snapshot.summaryText),
            numberless(snapshot.infoText),
            snapshot.iconDrawable.orEmpty(),
            numberless(snapshot.iconMatch),
            disposition,
            matchedRuleId.orEmpty(),
            versions.appVersionCode?.toString().orEmpty(),
            versions.sourceAppVersionCode?.toString().orEmpty(),
            versions.rulesetVersions.orEmpty(),
        )
        return Hashing.sha256Hex(parts.joinToString(SEPARATOR))
    }

    private const val SEPARATOR = "\u001f"
}
