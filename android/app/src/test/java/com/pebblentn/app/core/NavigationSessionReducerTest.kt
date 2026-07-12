package com.pebblentn.app.core

import com.pebblentn.app.protocol.Protocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationSessionReducerTest {

    private val reducer = NavigationSessionReducer

    private fun instr(maneuver: Maneuver, distance: Int? = null, primary: String? = null) =
        NavigationInstruction(maneuver = maneuver, distanceMeters = distance, primaryText = primary)

    private fun sendEffects(result: ReducerResult) =
        result.effects.filterIsInstance<ReducerEffect.SendState>()

    private fun hasFlag(flags: Int, mask: Int) = flags and mask != 0

    // --- Session start / launch-once -------------------------------------------------------------

    @Test
    fun firstInstructionBeforeReadyLaunchesOnceAndDefersSend() {
        val result = reducer.reduce(
            ReducerState(),
            ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 100), atEpochSeconds = 0),
        )

        assertTrue(result.effects.contains(ReducerEffect.LaunchWatchApp))
        assertTrue("no send before watch is ready", sendEffects(result).isEmpty())
        val current = result.state.current
        assertTrue(current is NavigationState.Navigating)
        assertEquals(1, (current as NavigationState.Navigating).sessionId)
        assertEquals(1, result.state.launchedSessionId)
        assertEquals(2, result.state.nextSessionId)
    }

    @Test
    fun watchReadyAfterLaunchSendsCurrentNavigating() {
        var state = ReducerState()
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 100), 0)).state
        val result = reducer.reduce(state, ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, atEpochSeconds = 1))

        val sends = sendEffects(result)
        assertEquals(1, sends.size)
        assertTrue(sends.single().state is NavigationState.Navigating)
    }

    @Test
    fun readyWatchThenInstructionLaunchesAndSendsWithManeuverVibrate() {
        var state = ReducerState()
        state = reducer.reduce(state, ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, 0)).state
        val result = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.LEFT, 200), 1))

        assertTrue(result.effects.contains(ReducerEffect.LaunchWatchApp))
        val send = sendEffects(result).single()
        assertTrue(
            "first maneuver of a session vibrates when enabled",
            hasFlag(send.flags, Protocol.FlagMasks.VIBRATE_ON_MANEUVER_CHANGE_MASK),
        )
    }

    @Test
    fun launchHappensOnlyOncePerSession() {
        var state = reducer.reduce(ReducerState(), ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, 0)).state
        val first = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.LEFT, 300), 1))
        assertTrue(first.effects.contains(ReducerEffect.LaunchWatchApp))
        state = first.state
        val second = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 200), 2))
        assertFalse("must not relaunch mid-session", second.effects.contains(ReducerEffect.LaunchWatchApp))
    }

    // --- Deduplication / pending replacement -----------------------------------------------------

    @Test
    fun identicalQuantizedInstructionIsDeduplicated() {
        var state = reducer.reduce(ReducerState(), ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, 0)).state
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 103), 1)).state
        // 108 quantizes to the same 100 -> no material change -> no send.
        val result = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 108), 2))
        assertTrue("duplicate should not be resent", sendEffects(result).isEmpty())
    }

    @Test
    fun distanceChangeBeyondQuantumIsSentWithoutVibration() {
        var state = reducer.reduce(ReducerState(), ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, 0)).state
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 100), 1)).state
        val result = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 300), 2))
        val send = sendEffects(result).single()
        assertFalse(
            "same maneuver must not vibrate",
            hasFlag(send.flags, Protocol.FlagMasks.VIBRATE_ON_MANEUVER_CHANGE_MASK),
        )
    }

    @Test
    fun maneuverChangeVibratesWhenEnabled() {
        var state = reducer.reduce(ReducerState(), ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, 0)).state
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 100), 1)).state
        val result = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.LEFT, 100), 2))
        val send = sendEffects(result).single()
        assertTrue(hasFlag(send.flags, Protocol.FlagMasks.VIBRATE_ON_MANEUVER_CHANGE_MASK))
    }

    @Test
    fun vibrationSuppressedWhenSettingDisabled() {
        val settings = WatchSettings.DEFAULT.copy(vibrateOnManeuverChange = false)
        var state = ReducerState(settings = settings)
        state = reducer.reduce(state, ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, 0)).state
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 100), 1)).state
        val result = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.LEFT, 100), 2))
        val send = sendEffects(result).single()
        assertFalse(hasFlag(send.flags, Protocol.FlagMasks.VIBRATE_ON_MANEUVER_CHANGE_MASK))
    }

    // --- Stop behavior ---------------------------------------------------------------------------

    @Test
    fun stopSendsStoppedWithExitFlagAndResetsLaunch() {
        var state = reducer.reduce(ReducerState(), ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, 0)).state
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 100), 1)).state
        val result = reducer.reduce(state, ReducerEvent.NavigationStopped(2))

        val send = sendEffects(result).single()
        assertTrue(send.state is NavigationState.Stopped)
        assertTrue(hasFlag(send.flags, Protocol.FlagMasks.EXIT_TO_WATCHFACE_ON_STOP_MASK))
        assertEquals(null, result.state.launchedSessionId)
        assertEquals(null, result.state.lastSentInstruction)
    }

    @Test
    fun stopWithoutActiveSessionIsNoOp() {
        val result = reducer.reduce(ReducerState(), ReducerEvent.NavigationStopped(1))
        assertTrue(result.effects.isEmpty())
        assertTrue(result.state.current is NavigationState.NoActiveNavigation)
    }

    @Test
    fun stopWithExitDisabledClearsExitFlag() {
        val settings = WatchSettings.DEFAULT.copy(exitToWatchfaceOnStop = false)
        var state = ReducerState(settings = settings)
        state = reducer.reduce(state, ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, 0)).state
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 100), 1)).state
        val result = reducer.reduce(state, ReducerEvent.NavigationStopped(2))
        assertFalse(hasFlag(sendEffects(result).single().flags, Protocol.FlagMasks.EXIT_TO_WATCHFACE_ON_STOP_MASK))
    }

    @Test
    fun newSessionAfterStopGetsNewIdAndRelaunches() {
        var state = reducer.reduce(ReducerState(), ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, 0)).state
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 100), 1)).state
        state = reducer.reduce(state, ReducerEvent.NavigationStopped(2)).state
        val result = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.LEFT, 100), 3))

        assertTrue(result.effects.contains(ReducerEffect.LaunchWatchApp))
        val current = result.state.current as NavigationState.Navigating
        assertEquals(2, current.sessionId)
    }

    // --- Protocol compatibility ------------------------------------------------------------------

    @Test
    fun incompatibleMajorProducesCompatibilityErrorAndNoState() {
        val result = reducer.reduce(
            ReducerState(),
            ReducerEvent.WatchReady(Protocol.MAJOR + 1, Protocol.MINOR, 0),
        )
        assertEquals(
            listOf(ReducerEffect.SendCompatibilityError(Protocol.ErrorCodes.INCOMPATIBLE_PROTOCOL_MAJOR)),
            result.effects,
        )
        assertTrue(result.state.watchReady)
        assertFalse(result.state.watchCompatible)
    }

    @Test
    fun incompatibleWatchDoesNotReceiveInstructions() {
        var state = reducer.reduce(ReducerState(), ReducerEvent.WatchReady(Protocol.MAJOR + 1, Protocol.MINOR, 0)).state
        val result = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 100), 1))
        assertTrue(sendEffects(result).isEmpty())
    }

    // --- Ready / request / idle ------------------------------------------------------------------

    @Test
    fun readyWhenIdleSendsNoActiveNavigation() {
        val result = reducer.reduce(ReducerState(), ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, 0))
        val send = sendEffects(result).single()
        assertEquals(NavigationState.NoActiveNavigation, send.state)
        assertEquals(0, send.flags)
    }

    @Test
    fun requestStateResendsCurrentToReadyWatch() {
        var state = reducer.reduce(ReducerState(), ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, 0)).state
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 100), 1)).state
        val result = reducer.reduce(state, ReducerEvent.WatchRequestedState(2))
        assertTrue(sendEffects(result).single().state is NavigationState.Navigating)
    }

    @Test
    fun requestStateBeforeReadyIsIgnored() {
        val result = reducer.reduce(ReducerState(), ReducerEvent.WatchRequestedState(0))
        assertTrue(result.effects.isEmpty())
    }

    // --- Staleness -------------------------------------------------------------------------------

    @Test
    fun freshnessCheckMarksStaleAndResendsWithStaleFlag() {
        var state = reducer.reduce(ReducerState(), ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, 0)).state
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 100), 0)).state
        val result = reducer.reduce(
            state,
            ReducerEvent.FreshnessChecked(NavigationSessionReducer.STALE_AFTER_SECONDS + 1),
        )
        val send = sendEffects(result).single()
        assertTrue(hasFlag(send.flags, Protocol.FlagMasks.STATE_IS_STALE_MASK))
        assertTrue((result.state.current as NavigationState.Navigating).stale)
    }

    @Test
    fun freshnessCheckWithinBudgetDoesNothing() {
        var state = reducer.reduce(ReducerState(), ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, 0)).state
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 100), 0)).state
        val result = reducer.reduce(state, ReducerEvent.FreshnessChecked(5))
        assertTrue(result.effects.isEmpty())
    }

    // --- Connection loss / restoration -----------------------------------------------------------

    @Test
    fun connectionLossDefersSendsUntilReadyThenSyncsCurrent() {
        var state = reducer.reduce(ReducerState(), ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, 0)).state
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 100), 1)).state
        state = reducer.reduce(state, ReducerEvent.ConnectionLost).state
        assertFalse(state.watchReady)

        val whileDown = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.LEFT, 200), 2))
        assertTrue("no sends while disconnected", sendEffects(whileDown).isEmpty())
        state = whileDown.state

        val reconnect = reducer.reduce(state, ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, 3))
        val send = sendEffects(reconnect).single()
        val navigating = send.state as NavigationState.Navigating
        assertEquals(Maneuver.LEFT, navigating.instruction.maneuver)
    }
}
