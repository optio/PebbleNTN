package com.pebblentn.app.notification

import com.pebblentn.app.core.AppAllowlist

/**
 * The privacy gate, isolated from the Android [android.service.notification.NotificationListenerService]
 * so it can be unit-tested without instrumentation.
 *
 * It receives only the package name (the sole datum needed to allowlist) and, for posts, a metadata
 * timestamp. It consults [allowlist] **before** enqueuing any work, so a disabled package never
 * reaches the [processor] and no notification content is ever read for it (REQ-ANDROID-003).
 */
class NotificationDispatcher(
    private val allowlist: AppAllowlist,
    private val queue: SerialProcessingQueue,
    private val processor: NotificationProcessor,
) {
    fun onPosted(packageName: String?, postedAtMillis: Long) {
        if (packageName == null || !allowlist.isEnabled(packageName)) return
        queue.enqueue { processor.onEligiblePosted(packageName, postedAtMillis) }
    }

    fun onRemoved(packageName: String?) {
        if (packageName == null || !allowlist.isEnabled(packageName)) return
        queue.enqueue { processor.onEligibleRemoved(packageName) }
    }
}
