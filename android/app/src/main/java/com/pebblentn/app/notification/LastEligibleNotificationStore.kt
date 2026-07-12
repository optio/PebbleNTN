package com.pebblentn.app.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory record of the most recent eligible notification, surfaced on the dashboard
 * ("last eligible notification timestamp", spec/400-ui/AndroidUI.md). Holds no content — only a
 * timestamp — so it carries no privacy risk.
 */
class LastEligibleNotificationStore {
    private val _lastEligibleAtMillis = MutableStateFlow<Long?>(null)
    val lastEligibleAtMillis: StateFlow<Long?> = _lastEligibleAtMillis.asStateFlow()

    fun record(atMillis: Long) {
        _lastEligibleAtMillis.value = atMillis
    }
}
