package com.pebblentn.app.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The most recent eligible notification, surfaced on the dashboard ("last eligible notification
 * timestamp", spec/400-ui/AndroidUI.md). Holds no content, only a timestamp, so it carries no
 * privacy risk. Kept in memory and [seed]ed at startup from the debug history, so an app restart
 * no longer shows "No eligible notifications yet" while the history has events (#28).
 */
class LastEligibleNotificationStore {
    private val _lastEligibleAtMillis = MutableStateFlow<Long?>(null)
    val lastEligibleAtMillis: StateFlow<Long?> = _lastEligibleAtMillis.asStateFlow()

    fun record(atMillis: Long) {
        _lastEligibleAtMillis.value = atMillis
    }

    /** Restore a stored timestamp; never replaces a newer one recorded since the app started. */
    fun seed(atMillis: Long) {
        _lastEligibleAtMillis.update { current -> if (current == null || current < atMillis) atMillis else current }
    }
}
