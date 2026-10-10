package com.pebblentn.app.notification

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.pebblentn.app.notification.icon.IconMatch
import com.pebblentn.app.notification.icon.LargeIconRecognizer

/**
 * Builds a [NotificationSnapshot] from a notification by reading only the documented selected
 * extras. It never touches actions, content/PendingIntents, RemoteViews or unlisted extras, and reads
 * the large icon only through [LargeIconRecognizer], which keeps nothing but a drawable name
 * (REQ-SEC-003, #74). Callers must have already passed the package allowlist before invoking this.
 */
object NotificationSnapshotFactory {

    fun create(sbn: StatusBarNotification, iconRecognizer: LargeIconRecognizer? = null): NotificationSnapshot =
        create(sbn.packageName, sbn.id, sbn.postTime, sbn.notification, iconRecognizer)

    fun create(
        packageName: String,
        notificationId: Int,
        postTimeMillis: Long,
        notification: Notification,
        iconRecognizer: LargeIconRecognizer? = null,
    ): NotificationSnapshot {
        val extras = notification.extras
        fun str(key: String): String? = extras?.getCharSequence(key)?.toString()?.takeIf { it.isNotEmpty() }
        // Documented numeric progress, only when actually present (getInt returns 0 for a missing key).
        fun int(key: String): Int? = if (extras?.containsKey(key) == true) extras.getInt(key) else null

        return NotificationSnapshot(
            packageName = packageName,
            notificationId = notificationId,
            channelId = notification.channelId,
            category = notification.category,
            template = extras?.getString(Notification.EXTRA_TEMPLATE),
            postTimeMillis = postTimeMillis,
            whenTimeMillis = notification.`when`.takeIf { it != 0L },
            title = str(Notification.EXTRA_TITLE),
            text = str(Notification.EXTRA_TEXT),
            subText = str(Notification.EXTRA_SUB_TEXT),
            bigText = str(Notification.EXTRA_BIG_TEXT),
            summaryText = str(Notification.EXTRA_SUMMARY_TEXT),
            infoText = str(Notification.EXTRA_INFO_TEXT),
            progress = int(Notification.EXTRA_PROGRESS),
            progressMax = int(Notification.EXTRA_PROGRESS_MAX),
        ).withIcon(runCatching { iconRecognizer?.recognize(packageName, notification) }.getOrNull())
    }

    private fun NotificationSnapshot.withIcon(match: IconMatch?): NotificationSnapshot = when (match) {
        null -> this
        is IconMatch.Matched -> copy(iconDrawable = match.drawable, iconMatch = match.diagnostic)
        is IconMatch.NoMatch -> copy(iconMatch = match.diagnostic)
    }
}
