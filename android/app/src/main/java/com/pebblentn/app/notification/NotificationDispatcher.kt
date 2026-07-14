package com.pebblentn.app.notification

import com.pebblentn.app.core.AppAllowlist

/**
 * The privacy gate, isolated from the Android [android.service.notification.NotificationListenerService]
 * so it can be unit-tested without instrumentation.
 *
 * Two checks, in this order, both on the package name alone:
 *  1. [appEnabled] — the user's master switch (REQ-ANDROID-011). Off means the app does nothing.
 *  2. [allowlist] — the per-package allowlist (REQ-ANDROID-003).
 *
 * Both run **before** the [buildEligible] provider is ever invoked. That provider is what reads
 * notification content to build the snapshot, so for a disabled app or a disabled package it is
 * never called and no content is read. Eligible work is handed to a serialized queue off the
 * callback thread (REQ-ANDROID-006).
 */
class NotificationDispatcher(
    private val allowlist: AppAllowlist,
    private val queue: SerialProcessingQueue,
    private val processor: NotificationProcessor,
    /** The master switch; must not do I/O — it is read on the callback thread. */
    private val appEnabled: () -> Boolean = { true },
) {
    /** @param buildEligible reads content to assemble the snapshot; only called if eligible. */
    fun onPosted(packageName: String?, buildEligible: () -> PostedNotification) {
        if (!appEnabled()) return
        if (packageName == null || !allowlist.isEnabled(packageName)) return
        queue.enqueue { processor.onPosted(buildEligible()) }
    }

    fun onRemoved(packageName: String?) {
        if (!appEnabled()) return
        if (packageName == null || !allowlist.isEnabled(packageName)) return
        queue.enqueue { processor.onRemoved(packageName) }
    }
}
