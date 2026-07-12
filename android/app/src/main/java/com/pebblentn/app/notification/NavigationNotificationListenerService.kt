package com.pebblentn.app.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.pebblentn.app.PebbleNtnApplication

/**
 * The system notification listener (REQ-ANDROID-002). Deliberately thin: it extracts only the
 * package name (and, for posts, the metadata post time) and delegates to [NotificationDispatcher],
 * which applies the allowlist **before** any content could be read (REQ-ANDROID-003). No permanent
 * foreground service is used.
 */
class NavigationNotificationListenerService : NotificationListenerService() {

    private val dispatcher: NotificationDispatcher
        get() = (application as PebbleNtnApplication).container.notificationDispatcher

    override fun onListenerConnected() {
        super.onListenerConnected()
        (application as PebbleNtnApplication).container.onListenerConnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        // The snapshot lambda reads content; the dispatcher only invokes it if the package is
        // allowlisted, so content is never read for a disabled package (REQ-ANDROID-003).
        dispatcher.onPosted(sbn.packageName) {
            PostedNotification(
                snapshot = NotificationSnapshotFactory.create(sbn),
                notificationKey = sbn.key,
                tag = sbn.tag,
                receivedAtMillis = System.currentTimeMillis(),
            )
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        dispatcher.onRemoved(sbn.packageName)
    }
}
