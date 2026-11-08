package com.pebblentn.app.notification

import com.pebblentn.app.core.EpochClock
import com.pebblentn.app.core.NavigationInstruction
import com.pebblentn.app.core.SpeedSample
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
    /** Invoked with the normalized instruction of a matched rule (forwarded to the watch). */
    private val onMatchedInstruction: suspend (NavigationInstruction) -> Unit = {},
    /** Invoked when a source app's notification is removed (candidate session end). */
    private val onSessionEnded: suspend () -> Unit = {},
    /**
     * Invoked, after the instruction, with the raw speed fields of a notification that carries
     * progress values (#132 FR5): the reducer quantizes distances, so speed needs these.
     */
    private val onSpeedSample: suspend (SpeedSample) -> Unit = {},
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
        evaluation.instruction?.let { onMatchedInstruction(it) }
        speedSampleOf(event.snapshot, hasInstruction = evaluation.instruction != null)?.let { onSpeedSample(it) }
    }

    private fun speedSampleOf(snapshot: NotificationSnapshot, hasInstruction: Boolean): SpeedSample? {
        if (snapshot.progress == null || snapshot.progressMax == null) return null
        return SpeedSample(
            postTimeMillis = snapshot.postTimeMillis,
            progress = snapshot.progress,
            progressMax = snapshot.progressMax,
            subText = snapshot.subText,
            packageName = snapshot.packageName,
            hasInstruction = hasInstruction,
        )
    }

    override suspend fun onRemoved(packageName: String) {
        onSessionEnded()
    }
}
