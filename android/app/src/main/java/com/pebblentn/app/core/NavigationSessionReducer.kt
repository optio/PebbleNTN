package com.pebblentn.app.core

import com.pebblentn.app.protocol.Protocol

/**
 * Inputs to the reducer. Every notification and transport signal is converted into one of these
 * typed events before it reaches [NavigationSessionReducer], so all session logic lives in one
 * deterministic place (spec/200-architecture/System.md).
 */
sealed interface ReducerEvent {
    /** A notification produced a normalized instruction at [atEpochSeconds] (Unix seconds). */
    data class InstructionReceived(
        val instruction: NavigationInstruction,
        val atEpochSeconds: Long,
    ) : ReducerEvent

    /** The source app signalled navigation ended (notification removed / explicit stop). */
    data class NavigationStopped(val atEpochSeconds: Long) : ReducerEvent

    /** The watch sent WATCH_READY after opening AppMessage. */
    data class WatchReady(
        val protocolMajor: Int,
        val protocolMinor: Int,
        val atEpochSeconds: Long,
    ) : ReducerEvent

    /** The watch asked the phone to (re)send the current state. */
    data class WatchRequestedState(val atEpochSeconds: Long) : ReducerEvent

    /** A freshness check tick (e.g. on a bounded timer or before a resend). */
    data class FreshnessChecked(val atEpochSeconds: Long) : ReducerEvent

    /** The watch connection was lost. */
    data object ConnectionLost : ReducerEvent

    /** User changed watch-related settings. */
    data class SettingsChanged(val settings: WatchSettings) : ReducerEvent
}

/** Side effects the reducer asks the surrounding code to perform. The reducer itself is pure. */
sealed interface ReducerEffect {
    /** Launch the watchapp once for the current session (REQ-WATCH-005/006). */
    data object LaunchWatchApp : ReducerEffect

    /** Send [state] to the watch with the already-computed [flags] bitfield. */
    data class SendState(val state: NavigationState, val flags: Int) : ReducerEffect

    /** Tell the watch the phone protocol is incompatible (REQ-WATCH-010). */
    data class SendCompatibilityError(val errorCode: Int) : ReducerEffect
}

/**
 * Immutable reducer state. Holds everything needed to decide the next action without consulting any
 * other service — there is no session flag distributed elsewhere.
 */
data class ReducerState(
    val settings: WatchSettings = WatchSettings.DEFAULT,
    val current: NavigationState = NavigationState.NoActiveNavigation,
    val nextSessionId: Int = 1,
    /** Session id we have already auto-launched the watchapp for, if any. */
    val launchedSessionId: Int? = null,
    val watchReady: Boolean = false,
    val watchCompatible: Boolean = true,
    /** Last instruction actually sent to the watch, for dedup and maneuver-change detection. */
    val lastSentInstruction: NavigationInstruction? = null,
)

/** Result of a single reduction: the next [state] and the [effects] to run in order. */
data class ReducerResult(
    val state: ReducerState,
    val effects: List<ReducerEffect>,
)

/**
 * Deterministic session reducer. Owns session start/end detection, deduplication, distance
 * quantization, launch-once, READY synchronization, stale detection and stop behavior.
 *
 * Pure: `reduce(state, event)` returns a new state and a list of effects; it performs no I/O.
 */
object NavigationSessionReducer {

    /** A Navigating state older than this (seconds) is considered stale. */
    const val STALE_AFTER_SECONDS: Long = 30

    fun reduce(state: ReducerState, event: ReducerEvent): ReducerResult = when (event) {
        is ReducerEvent.InstructionReceived -> onInstruction(state, event)
        is ReducerEvent.NavigationStopped -> onStopped(state, event)
        is ReducerEvent.WatchReady -> onWatchReady(state, event)
        is ReducerEvent.WatchRequestedState -> onRequestState(state, event)
        is ReducerEvent.FreshnessChecked -> onFreshnessChecked(state, event)
        ReducerEvent.ConnectionLost -> ReducerResult(state.copy(watchReady = false), emptyList())
        is ReducerEvent.SettingsChanged -> ReducerResult(state.copy(settings = event.settings), emptyList())
    }

    private fun onInstruction(state: ReducerState, event: ReducerEvent.InstructionReceived): ReducerResult {
        val instruction = quantized(event.instruction)
        val current = state.current

        // Same session if already navigating; otherwise a fresh session begins.
        val sessionId: Int
        val nextSessionId: Int
        if (current is NavigationState.Navigating) {
            sessionId = current.sessionId
            nextSessionId = state.nextSessionId
        } else {
            sessionId = state.nextSessionId
            nextSessionId = state.nextSessionId + 1
        }

        val navigating = NavigationState.Navigating(
            sessionId = sessionId,
            instruction = instruction,
            stateTimestampSeconds = event.atEpochSeconds,
            stale = false,
        )

        val effects = mutableListOf<ReducerEffect>()

        // Launch once per session, if enabled. Launch even before the watch is ready — launching is
        // how the watchapp starts and then reports READY.
        var launchedSessionId = state.launchedSessionId
        if (state.settings.autoLaunchOnSessionStart && launchedSessionId != sessionId) {
            effects += ReducerEffect.LaunchWatchApp
            launchedSessionId = sessionId
        }

        // Decide whether this is a material change worth sending.
        val wasNavigatingSameSession = current is NavigationState.Navigating && current.sessionId == sessionId
        val materialChange = !wasNavigatingSameSession || state.lastSentInstruction != instruction

        var lastSent = state.lastSentInstruction
        if (state.watchReady && state.watchCompatible && materialChange) {
            val maneuverChanged = state.lastSentInstruction?.maneuver != instruction.maneuver
            effects += ReducerEffect.SendState(navigating, flagsForNavigating(state.settings, stale = false, maneuverChanged))
            lastSent = instruction
        }

        return ReducerResult(
            state = state.copy(
                current = navigating,
                nextSessionId = nextSessionId,
                launchedSessionId = launchedSessionId,
                lastSentInstruction = lastSent,
            ),
            effects = effects,
        )
    }

    private fun onStopped(state: ReducerState, event: ReducerEvent.NavigationStopped): ReducerResult {
        val current = state.current
        if (current !is NavigationState.Navigating) {
            // Idempotent: nothing to stop.
            return ReducerResult(state, emptyList())
        }
        val stopped = NavigationState.Stopped(current.sessionId)
        val effects = mutableListOf<ReducerEffect>()
        if (state.watchReady && state.watchCompatible) {
            effects += ReducerEffect.SendState(stopped, flagsForStopped(state.settings))
        }
        return ReducerResult(
            state = state.copy(
                current = stopped,
                launchedSessionId = null,
                lastSentInstruction = null,
            ),
            effects = effects,
        )
    }

    private fun onWatchReady(state: ReducerState, event: ReducerEvent.WatchReady): ReducerResult {
        if (event.protocolMajor != Protocol.MAJOR) {
            return ReducerResult(
                state = state.copy(watchReady = true, watchCompatible = false),
                effects = listOf(ReducerEffect.SendCompatibilityError(Protocol.ErrorCodes.INCOMPATIBLE_PROTOCOL_MAJOR)),
            )
        }
        val ready = state.copy(watchReady = true, watchCompatible = true)
        return sendCurrent(ready, event.atEpochSeconds)
    }

    private fun onRequestState(state: ReducerState, event: ReducerEvent.WatchRequestedState): ReducerResult {
        if (!state.watchReady || !state.watchCompatible) {
            return ReducerResult(state, emptyList())
        }
        return sendCurrent(state, event.atEpochSeconds)
    }

    private fun onFreshnessChecked(state: ReducerState, event: ReducerEvent.FreshnessChecked): ReducerResult {
        val current = state.current
        if (current !is NavigationState.Navigating) return ReducerResult(state, emptyList())
        val nowStale = isStale(current.stateTimestampSeconds, event.atEpochSeconds)
        if (current.stale == nowStale) return ReducerResult(state, emptyList())

        val updated = current.copy(stale = nowStale)
        val effects = if (state.watchReady && state.watchCompatible) {
            listOf(ReducerEffect.SendState(updated, flagsForNavigating(state.settings, stale = nowStale, maneuverChanged = false)))
        } else {
            emptyList()
        }
        return ReducerResult(state.copy(current = updated), effects)
    }

    /** Send whatever the phone currently believes to a ready, compatible watch. */
    private fun sendCurrent(state: ReducerState, atEpochSeconds: Long): ReducerResult {
        return when (val current = state.current) {
            is NavigationState.Navigating -> {
                val stale = isStale(current.stateTimestampSeconds, atEpochSeconds)
                val updated = current.copy(stale = stale)
                ReducerResult(
                    state = state.copy(current = updated, lastSentInstruction = current.instruction),
                    effects = listOf(
                        ReducerEffect.SendState(
                            updated,
                            flagsForNavigating(state.settings, stale, maneuverChanged = false),
                        ),
                    ),
                )
            }
            // After a stop or when idle, a (re)connecting watch is told there is no active navigation.
            NavigationState.NoActiveNavigation,
            is NavigationState.Stopped ->
                ReducerResult(state, listOf(ReducerEffect.SendState(NavigationState.NoActiveNavigation, flags = 0)))
        }
    }

    private fun quantized(instruction: NavigationInstruction): NavigationInstruction =
        instruction.distanceMeters?.let { instruction.copy(distanceMeters = DistanceQuantizer.quantize(it)) }
            ?: instruction

    private fun isStale(stateTimestampSeconds: Long, atEpochSeconds: Long): Boolean =
        atEpochSeconds - stateTimestampSeconds > STALE_AFTER_SECONDS

    private fun flagsForNavigating(settings: WatchSettings, stale: Boolean, maneuverChanged: Boolean): Int {
        var flags = 0
        if (stale) flags = flags or Protocol.FlagMasks.STATE_IS_STALE_MASK
        if (settings.activateBacklight) flags = flags or Protocol.FlagMasks.ACTIVATE_BACKLIGHT_MASK
        if (settings.vibrateOnManeuverChange && maneuverChanged) {
            flags = flags or Protocol.FlagMasks.VIBRATE_ON_MANEUVER_CHANGE_MASK
        }
        return flags
    }

    private fun flagsForStopped(settings: WatchSettings): Int =
        if (settings.exitToWatchfaceOnStop) Protocol.FlagMasks.EXIT_TO_WATCHFACE_ON_STOP_MASK else 0
}
