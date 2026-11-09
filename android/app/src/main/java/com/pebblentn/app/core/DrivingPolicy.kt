package com.pebblentn.app.core

/**
 * The settings the hide/show decision reads (#132): taken from the watch settings mirror and the
 * phone's auto-launch flag at the moment of each decision.
 *
 * @property hideEnabled When driving is "Hide directions" on a watchapp that has the setting.
 * @property thresholdKmh the driving speed of the unit in use, in km/h.
 * @property marginKmh how far below the threshold the speed must drop to show again (5 km/h, 3 mph).
 * @property autoLaunch whether "Launch watchapp when navigation starts" is on (FR9.1).
 */
data class DrivingConfig(
    val hideEnabled: Boolean,
    val thresholdKmh: Double,
    val marginKmh: Double,
    val autoLaunch: Boolean,
) {
    companion object {
        /** Hiding off: what a 1.3 watchapp, unknown settings and "Show directions" all mean. */
        val OFF = DrivingConfig(hideEnabled = false, thresholdKmh = 0.0, marginKmh = 0.0, autoLaunch = true)

        /** The config for [model]'s When driving settings, or [OFF] when the watchapp lacks them. */
        fun from(model: WatchSettingsModel?, hasDrivingSettings: Boolean, autoLaunch: Boolean): DrivingConfig {
            if (model == null || !hasDrivingSettings) return OFF.copy(autoLaunch = autoLaunch)
            val imperial = DrivingSpeeds.isImperial(model.units)
            return DrivingConfig(
                hideEnabled = model.drivingHide,
                thresholdKmh = model.drivingThresholdKmh(),
                marginKmh = if (imperial) DrivingPolicy.MARGIN_MPH * DrivingSpeeds.KMH_PER_MPH else DrivingPolicy.MARGIN_KMH,
                autoLaunch = autoLaunch,
            )
        }
    }
}

/**
 * Where the hide/show logic stands within one navigation session.
 *
 * @property hidden the phone closed the watchapp for driving and has not shown it since (FR8).
 * @property keptOpen the user opened the watchapp while it was hidden: no more hiding this session (FR10).
 * @property aboveSince when the measured speed went above the threshold, while visible.
 * @property belowSince when it went below the threshold minus the margin, while hidden.
 * @property lastHideAt / [lastShowAt] the last hide and show, for the 2-minute limit (NFR2).
 */
data class DrivingState(
    val hidden: Boolean = false,
    val keptOpen: Boolean = false,
    val aboveSince: Long? = null,
    val belowSince: Long? = null,
    val lastHideAt: Long? = null,
    val lastShowAt: Long? = null,
)

/** What the controller does after a decision. */
enum class DrivingAction {
    NONE,

    /** Close the watchapp and stop sending navigation updates (FR7, FR8). */
    HIDE,

    /** Reopen the watchapp with the current instruction (FR9, FR12). */
    SHOW,

    /** Leave the hidden state without reopening: auto-launch is off (FR9.1). */
    LEAVE_HIDDEN,
}

data class DrivingResult(val state: DrivingState, val action: DrivingAction)

/**
 * When to hide the watchapp while driving and when to show it again (#132 FR6–FR12, NFR2, NFR3):
 * pure functions of the state, the time, the speed estimate and the settings, like the session
 * reducer. The timing values are starting points to tune with real trips; they are not settings.
 *
 * - Visible: the measured speed above the threshold for [HIDE_AFTER_MILLIS] hides (FR7), or for
 *   [EARLY_HIDE_AFTER_MILLIS] when the planned speed is above [EARLY_HIDE_PLANNED_KMH].
 * - Hidden: the measured speed below the threshold minus the margin for [SHOW_AFTER_MILLIS] shows
 *   again (FR9); with auto-launch off it only leaves the hidden state (FR9.1).
 * - At most one hide and one show in any [MIN_REPEAT_MILLIS] (NFR2).
 * - "Show directions", or a watchapp without the setting, never hides and ends a hidden state (FR12).
 * - A user who opens the watchapp while it is hidden keeps it for the session (FR10); a session
 *   stop starts everything afresh (FR11).
 */
object DrivingPolicy {

    const val HIDE_AFTER_MILLIS = 30_000L
    const val EARLY_HIDE_AFTER_MILLIS = 10_000L
    const val EARLY_HIDE_PLANNED_KMH = 40.0
    const val SHOW_AFTER_MILLIS = 120_000L
    const val MIN_REPEAT_MILLIS = 120_000L
    const val MARGIN_KMH = 5.0
    const val MARGIN_MPH = 3.0

    /** Decide at [nowMillis] from [estimate] (null: no speed known) and [config]. */
    fun evaluate(state: DrivingState, nowMillis: Long, estimate: SpeedEstimate?, config: DrivingConfig): DrivingResult {
        if (state.keptOpen) return DrivingResult(state, DrivingAction.NONE)
        if (!config.hideEnabled) {
            val cleared = state.copy(aboveSince = null, belowSince = null)
            return if (state.hidden) leaveHidden(cleared, nowMillis, config) else DrivingResult(cleared, DrivingAction.NONE)
        }
        return if (state.hidden) whileHidden(state, nowMillis, estimate, config) else whileVisible(state, nowMillis, estimate, config)
    }

    /** The user opened the watchapp while it was hidden: it stays open this session (FR10). */
    fun onUserOpened(state: DrivingState): DrivingState =
        state.copy(hidden = false, keptOpen = true, aboveSince = null, belowSince = null)

    /** Navigation stopped: the next session starts fresh (FR11). */
    fun onSessionStopped(): DrivingState = DrivingState()

    private fun whileVisible(state: DrivingState, now: Long, estimate: SpeedEstimate?, config: DrivingConfig): DrivingResult {
        val measured = estimate?.measuredKmh
        if (measured == null || measured <= config.thresholdKmh) {
            return DrivingResult(state.copy(aboveSince = null), DrivingAction.NONE)
        }
        val aboveSince = state.aboveSince ?: now
        val planned = estimate.plannedKmh
        val needed = if (planned != null && planned > EARLY_HIDE_PLANNED_KMH) EARLY_HIDE_AFTER_MILLIS else HIDE_AFTER_MILLIS
        val allowed = state.lastHideAt == null || now - state.lastHideAt >= MIN_REPEAT_MILLIS
        if (now - aboveSince >= needed && allowed) {
            return DrivingResult(
                state.copy(hidden = true, aboveSince = null, belowSince = null, lastHideAt = now),
                DrivingAction.HIDE,
            )
        }
        return DrivingResult(state.copy(aboveSince = aboveSince), DrivingAction.NONE)
    }

    private fun whileHidden(state: DrivingState, now: Long, estimate: SpeedEstimate?, config: DrivingConfig): DrivingResult {
        val measured = estimate?.measuredKmh
        if (measured == null || measured >= config.thresholdKmh - config.marginKmh) {
            return DrivingResult(state.copy(belowSince = null), DrivingAction.NONE)
        }
        val belowSince = state.belowSince ?: now
        val allowed = state.lastShowAt == null || now - state.lastShowAt >= MIN_REPEAT_MILLIS
        if (now - belowSince >= SHOW_AFTER_MILLIS && allowed) {
            return leaveHidden(state.copy(belowSince = null), now, config)
        }
        return DrivingResult(state.copy(belowSince = belowSince), DrivingAction.NONE)
    }

    private fun leaveHidden(state: DrivingState, now: Long, config: DrivingConfig): DrivingResult = DrivingResult(
        state.copy(hidden = false, aboveSince = null, belowSince = null, lastShowAt = now),
        if (config.autoLaunch) DrivingAction.SHOW else DrivingAction.LEAVE_HIDDEN,
    )
}
