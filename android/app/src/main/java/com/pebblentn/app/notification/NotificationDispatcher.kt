package com.pebblentn.app.notification

import com.pebblentn.app.core.AppAllowlist

/**
 * The privacy gate, isolated from the Android [android.service.notification.NotificationListenerService]
 * so it can be unit-tested without instrumentation.
 *
 * It consults [allowlist] using only the package name, **before** the [buildEligible] provider is
 * ever invoked. That provider is what reads notification content to build the snapshot, so for a
 * disabled package it is never called and no content is read (REQ-ANDROID-003). Eligible work is
 * handed to a serialized queue off the callback thread (REQ-ANDROID-006).
 */
class NotificationDispatcher(
    private val allowlist: AppAllowlist,
    private val queue: SerialProcessingQueue,
    private val processor: NotificationProcessor,
) {
    /** @param buildEligible reads content to assemble the snapshot; only called if eligible. */
    fun onPosted(packageName: String?, buildEligible: () -> PostedNotification) {
        if (packageName == null || !allowlist.isEnabled(packageName)) return
        queue.enqueue { processor.onPosted(buildEligible()) }
    }

    fun onRemoved(packageName: String?) {
        if (packageName == null || !allowlist.isEnabled(packageName)) return
        queue.enqueue { processor.onRemoved(packageName) }
    }
}
