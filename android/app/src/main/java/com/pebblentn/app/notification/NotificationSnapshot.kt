package com.pebblentn.app.notification

import kotlinx.serialization.Serializable

/**
 * The immutable, minimal record of an eligible notification (REQ-SEC-003, spec/300-data).
 *
 * It holds only the documented selected fields. It deliberately has no place for PendingIntents,
 * actions, RemoteViews, icon images, people/contact identifiers or arbitrary extras — those are never
 * extracted, so they cannot be stored or exported. The one exception is narrow: for an app whose
 * rules map icons, the large icon is compared on the device with that app's own turn drawables, and
 * only the matched drawable's name is kept ([iconDrawable]), never the image (REQ-SEC-003, #74).
 */
@Serializable
data class NotificationSnapshot(
    val packageName: String,
    val notificationId: Int,
    val channelId: String? = null,
    val category: String? = null,
    val template: String? = null,
    val postTimeMillis: Long = 0,
    val whenTimeMillis: Long? = null,
    val title: String? = null,
    val text: String? = null,
    val subText: String? = null,
    val bigText: String? = null,
    val summaryText: String? = null,
    val infoText: String? = null,
    /**
     * The notification's documented numeric progress, when present (`EXTRA_PROGRESS` /
     * `EXTRA_PROGRESS_MAX`). Captured for diagnosis of the Android 16 `ProgressStyle` navigation
     * notification, whose maneuver distance is not in any readable text field. These are non-content,
     * non-personal integers (a whole-trip tracker position), so they carry no destination or identity
     * (REQ-SEC-003). They are NOT part of [combinedText] and are not used for distance extraction.
     */
    val progress: Int? = null,
    val progressMax: Int? = null,
    /**
     * The name of the navigation app's own turn drawable that its large icon matched, e.g.
     * `ic_turn_left` (#74), for apps whose direction is only in the icon (CoMaps, Organic Maps,
     * Google Maps' classic card). Rules map it to a maneuver with a `maneuverMap` on this field. A
     * resource name, not content: no destination or identity. Not part of [combinedText].
     */
    val iconDrawable: String? = null,
    /** Debug only: how the icon was matched, or why not ("no match: ambiguous …"). */
    val iconMatch: String? = null,
) {
    /**
     * Text fields joined for rule matching. Rules reference `combinedText` (see the bundled
     * example ruleset) as well as the individual named fields.
     */
    val combinedText: String
        get() = listOfNotNull(title, text, bigText, subText, summaryText, infoText)
            .filter { it.isNotBlank() }
            .joinToString(separator = " ")
}
