package com.pebblentn.app.notification

import com.pebblentn.app.core.EpochClock
import com.pebblentn.app.data.DebugHistoryRepository
import com.pebblentn.app.rules.RuleEngine
import com.pebblentn.app.rules.RuleRepository

/**
 * Processes each eligible posted notification: runs the rule engine against the snapshot and stores
 * the event with its match result and trace in debug history (REQ-DEBUG-001/002). Only reached for
 * allowlisted packages. The normalized instruction of a matched rule is forwarded to the
 * reducer/watch transport in a later milestone; here it is recorded for inspection.
 */
class DebugCaptureProcessor(
    private val debugHistory: DebugHistoryRepository,
    private val lastEligibleStore: LastEligibleNotificationStore,
    private val ruleEngine: RuleEngine,
    private val ruleRepository: RuleRepository,
    private val clock: EpochClock = EpochClock.SYSTEM,
    private val localeProvider: () -> String? = { null },
) : NotificationProcessor {

    override suspend fun onPosted(event: PostedNotification) {
        val evaluation = ruleEngine.evaluate(
            snapshot = event.snapshot,
            rules = ruleRepository.current(),
            locale = localeProvider(),
            nowEpochSeconds = clock.nowMillis() / 1000,
        )
        debugHistory.recordPosted(event, evaluation)
        lastEligibleStore.record(event.snapshot.postTimeMillis)
    }

    override suspend fun onRemoved(packageName: String) {
        // Session-end handling arrives with the reducer/transport wiring in a later milestone.
    }
}
