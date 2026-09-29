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

    // --- Auto-launch disabled setting (REQ-WATCH-005, Issue #9) -----------------------------------

    @Test
    fun autoLaunchDisabledDoesNotEmitLaunchEffectForGoogleMapsNotification() {
        val settings = WatchSettings.DEFAULT.copy(autoLaunchOnSessionStart = false)
        val googleMapsInstruction = instr(Maneuver.RIGHT, 100, "Turn right on Main St")
        val result = reducer.reduce(
            ReducerState(settings = settings),
            ReducerEvent.InstructionReceived(googleMapsInstruction, atEpochSeconds = 0),
        )

        assertFalse("must not launch watchapp when auto-launch is disabled", result.effects.contains(ReducerEffect.LaunchWatchApp))
        assertEquals(null, result.state.launchedSessionId)
        assertTrue(result.state.current is NavigationState.Navigating)
        assertEquals(1, (result.state.current as NavigationState.Navigating).sessionId)
    }

    @Test
    fun autoLaunchDisabledDoesNotEmitLaunchEffectForOsmAndNotification() {
        val settings = WatchSettings.DEFAULT.copy(autoLaunchOnSessionStart = false)
        val osmandInstruction = instr(Maneuver.UNKNOWN, 200, "Turn right and go")
        val result = reducer.reduce(
            ReducerState(settings = settings),
            ReducerEvent.InstructionReceived(osmandInstruction, atEpochSeconds = 0),
        )

        assertFalse("must not launch watchapp for OsmAnd when auto-launch is disabled", result.effects.contains(ReducerEffect.LaunchWatchApp))
        assertEquals(null, result.state.launchedSessionId)
        assertTrue(result.state.current is NavigationState.Navigating)
    }

    @Test
    fun autoLaunchDisabledThenManualWatchReadySendsCurrentStateAndContinuesForwarding() {
        val settings = WatchSettings.DEFAULT.copy(autoLaunchOnSessionStart = false)
        var state = ReducerState(settings = settings)
        // 1. Navigation starts: instruction received with auto-launch disabled
        val navStart = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 300), 0))
        assertFalse(navStart.effects.contains(ReducerEffect.LaunchWatchApp))
        assertTrue("no send before watch is ready", sendEffects(navStart).isEmpty())
        state = navStart.state

        // 2. User manually opens watchapp on watch (watch sends WATCH_READY)
        val readyResult = reducer.reduce(state, ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, atEpochSeconds = 1))
        val initialSend = sendEffects(readyResult).single()
        assertEquals(Maneuver.RIGHT, (initialSend.state as NavigationState.Navigating).instruction.maneuver)
        state = readyResult.state

        // 3. Subsequent navigation update arrives: data continues to be forwarded live
        val updateResult = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.LEFT, 150), 2))
        assertFalse("must not launch mid-session", updateResult.effects.contains(ReducerEffect.LaunchWatchApp))
        val updateSend = sendEffects(updateResult).single()
        assertEquals(Maneuver.LEFT, (updateSend.state as NavigationState.Navigating).instruction.maneuver)
    }

    @Test
    fun toggleAutoLaunchMidNavigationDoesNotKillOrStop() {
        var state = reducer.reduce(ReducerState(), ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, 0)).state
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(instr(Maneuver.RIGHT, 100), 1)).state

        // User toggles autoLaunch off mid-navigation
        val disabledSettings = state.settings.copy(autoLaunchOnSessionStart = false)
        val settingsChangedResult = reducer.reduce(state, ReducerEvent.SettingsChanged(disabledSettings))
        assertTrue("settings change must produce no side effects (no kill/stop)", settingsChangedResult.effects.isEmpty())
        assertTrue("navigation state must remain active", settingsChangedResult.state.current is NavigationState.Navigating)
        assertFalse(settingsChangedResult.state.settings.autoLaunchOnSessionStart)

        // Navigation updates continue normally
        val nextResult = reducer.reduce(settingsChangedResult.state, ReducerEvent.InstructionReceived(instr(Maneuver.LEFT, 50), 2))
        val send = sendEffects(nextResult).single()
        assertEquals(Maneuver.LEFT, (send.state as NavigationState.Navigating).instruction.maneuver)
    }

    // --- ETA carry-over (REQ-ANDROID-014) --------------------------------------------------------

    private fun eta(state: ReducerState) = (state.current as NavigationState.Navigating).instruction.secondaryText

    /** Walking: an overview card with the ETA, then a classic turn card without one. */
    private fun overview(etaText: String) =
        NavigationInstruction(Maneuver.STRAIGHT, primaryText = "Example Destination", secondaryText = etaText)

    private val turnCard = instr(Maneuver.LEFT, 50, "Turn left onto Example Street")

    private fun ready() = reducer.reduce(ReducerState(), ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, 0)).state

    @Test
    fun turnCardWithoutEtaReusesTheSessionsLastEta() {
        var state = reducer.reduce(ready(), ReducerEvent.InstructionReceived(overview("18:57"), 10)).state
        val result = reducer.reduce(state, ReducerEvent.InstructionReceived(turnCard, 20))

        assertEquals("18:57", eta(result.state))
        val sent = sendEffects(result).single().state as NavigationState.Navigating
        assertEquals("the watch receives the carried ETA", "18:57", sent.instruction.secondaryText)
        assertEquals(Maneuver.LEFT, sent.instruction.maneuver)
    }

    @Test
    fun anInstructionsOwnEtaWinsAndBecomesTheNewCarriedEta() {
        var state = reducer.reduce(ready(), ReducerEvent.InstructionReceived(overview("18:57"), 10)).state
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(overview("18:59"), 20)).state
        assertEquals("18:59", eta(state))
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(turnCard, 30)).state
        assertEquals("18:59", eta(state))
    }

    @Test
    fun etaEpochSecondsIsCarriedToo() {
        val withEpoch = NavigationInstruction(Maneuver.STRAIGHT, etaEpochSeconds = 1_800_000_000L)
        var state = reducer.reduce(ready(), ReducerEvent.InstructionReceived(withEpoch, 10)).state
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(turnCard, 20)).state
        assertEquals(1_800_000_000L, (state.current as NavigationState.Navigating).instruction.etaEpochSeconds)
    }

    @Test
    fun etaIsNeverCarriedIntoANewSession() {
        var state = reducer.reduce(ready(), ReducerEvent.InstructionReceived(overview("18:57"), 10)).state
        state = reducer.reduce(state, ReducerEvent.NavigationStopped(20)).state
        assertEquals(null, state.knownEta)
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(turnCard, 30)).state
        assertEquals(2, (state.current as NavigationState.Navigating).sessionId)
        assertEquals("a new route starts without the old ETA", null, eta(state))
    }

    @Test
    fun anEtaOlderThanTheLimitIsDropped() {
        val limit = NavigationSessionReducer.ETA_CARRY_MAX_AGE_SECONDS
        var state = reducer.reduce(ready(), ReducerEvent.InstructionReceived(overview("18:57"), 0)).state
        val atLimit = reducer.reduce(state, ReducerEvent.InstructionReceived(turnCard, limit)).state
        assertEquals("still carried at the limit", "18:57", eta(atLimit))
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(turnCard, limit + 1)).state
        assertEquals("dropped once older than the limit", null, eta(state))
    }

    @Test
    fun reusingAnEtaDoesNotRefreshItsAge() {
        val limit = NavigationSessionReducer.ETA_CARRY_MAX_AGE_SECONDS
        var state = reducer.reduce(ready(), ReducerEvent.InstructionReceived(overview("18:57"), 0)).state
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(turnCard, limit - 10)).state
        assertEquals("18:57", eta(state))
        state = reducer.reduce(state, ReducerEvent.InstructionReceived(turnCard.copy(distanceMeters = 20), limit + 1)).state
        assertEquals("age counts from when the ETA was seen, not reused", null, eta(state))
    }
}
