package com.pebblentn.app.core

/**
 * The phone's current belief about navigation, and the single value it synchronizes to the watch.
 *
 * This is a *state*, never a queue: the phone always holds exactly one current [NavigationState]
 * per the last-known-state principle (REQ-ANDROID-010, REQ-WATCH-004). Obsolete instructions are
 * replaced, never replayed.
 */
sealed interface NavigationState {

    /** No navigation session is active. Maps to the `NO_ACTIVE_NAVIGATION` event. */
    data object NoActiveNavigation : NavigationState

    /**
     * An active navigation session. Maps to the `NAVIGATION_UPDATE` event.
     *
     * @property sessionId monotonically increasing id identifying this session on the watch.
     * @property instruction the latest normalized instruction.
     * @property stateTimestampSeconds when this state was produced (Unix epoch seconds).
     * @property stale true when the state is older than the freshness budget and should be shown
     *   as potentially outdated (sets the `STATE_IS_STALE` flag).
     */
    data class Navigating(
        val sessionId: Int,
        val instruction: NavigationInstruction,
        val stateTimestampSeconds: Long,
        val stale: Boolean = false,
    ) : NavigationState

    /**
     * A session that has just ended. Maps to the `NAVIGATION_STOPPED` event; the watch returns to
     * the watchface when the exit flag is set.
     *
     * @property sessionId the session that ended.
     */
    data class Stopped(val sessionId: Int) : NavigationState
}
