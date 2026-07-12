package com.pebblentn.app.notification

import android.app.Notification
import android.service.notification.StatusBarNotification

/**
 * Builds a [NotificationSnapshot] from a notification by reading only the documented selected
 * extras. It never touches actions, content/PendingIntents, RemoteViews, icons or unlisted extras
 * (REQ-SEC-003). Callers must have already passed the package allowlist before invoking this.
 */
object NotificationSnapshotFactory {

    fun create(sbn: StatusBarNotification): NotificationSnapshot =
        create(sbn.packageName, sbn.id, sbn.postTime, sbn.notification)

    fun create(
        packageName: String,
        notificationId: Int,
        postTimeMillis: Long,
        notification: Notification,
    ): NotificationSnapshot {
        val extras = notification.extras
        fun str(key: String): String? = extras?.getCharSequence(key)?.toString()?.takeIf { it.isNotEmpty() }

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
        )
    }
}
