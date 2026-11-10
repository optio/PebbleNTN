package com.pebblentn.app.pebble

import com.pebblentn.app.core.DrivingAction
import com.pebblentn.app.core.DrivingConfig
import com.pebblentn.app.core.DrivingPolicy
import com.pebblentn.app.core.DrivingSpeedEstimator
import com.pebblentn.app.core.DrivingState
import com.pebblentn.app.core.EpochClock
import com.pebblentn.app.core.NavigationInstruction
import com.pebblentn.app.core.SpeedEstimate
import com.pebblentn.app.core.SpeedSample
import com.pebblentn.app.data.DrivingDecision
import com.pebblentn.app.data.DrivingLogEntry
import com.pebblentn.app.data.DrivingLogRepository
import kotlinx.coroutines.Job
import com.pebblentn.app.core.NavigationSessionReducer
import com.pebblentn.app.core.ReducerEffect
import com.pebblentn.app.core.ReducerEvent
import com.pebblentn.app.core.ReducerState
import com.pebblentn.app.core.WatchSetting
import com.pebblentn.app.core.WatchSettings
import com.pebblentn.app.core.WatchSettingsModel
import com.pebblentn.app.core.WatchSettingsSync
import com.pebblentn.app.data.InMemoryWatchSettingsStore
import com.pebblentn.app.data.NavigationStateRepository
import com.pebblentn.app.data.WatchSettingsStore
import com.pebblentn.app.protocol.AppMessage
import com.pebblentn.app.protocol.Protocol
import com.pebblentn.app.protocol.ProtocolCodec
import com.pebblentn.app.protocol.SendResult
import com.pebblentn.app.protocol.WatchTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import com.pebblentn.app.core.NavigationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Bridges the notification/rule pipeline and the watch: it feeds events into the pure
 * [NavigationSessionReducer], holds the single current [ReducerState], and runs the reducer's
 * effects against a [WatchTransport] (launch-once, send current state, compatibility error). Inbound
 * watch messages (WATCH_READY, WATCH_REQUEST_STATE) are decoded and dispatched too.
 *
 * All reducer access is serialized by a mutex so events from the listener, the watch and settings
 * never interleave. Failed sends are retried with bounded backoff while a newer state supersedes an
 * older pending one naturally, because the next reduction replaces what is sent.
 *
 * It also keeps the phone's mirror of the watchapp's own settings in sync (REQ-ANDROID-021, #102):
 * settings a real WATCH_READY or a SETTINGS message carries are merged by [WatchSettingsSync], and
 * phone edits go to the watch only while the watchapp is open, meaning it has sent its settings in
 * this session and has not closed since. Otherwise they wait in the [watchSettings] store for the
 * next time it opens; a send never relaunches the watchapp ([WatchTransport.sendToOpenApp]).
 */
class NavigationController(
    private val transport: WatchTransport,
    private val scope: CoroutineScope,
    private val appVersion: String,
    private val clock: EpochClock = EpochClock.SYSTEM,
    private val maxSendAttempts: Int = 3,
    private val baseBackoffMillis: Long = 50,
    private val readySettleMillis: Long = 1_500,
    private val stateStore: NavigationStateRepository? = null,
    initialSettings: WatchSettings = WatchSettings.DEFAULT,
    /** Called when a message reached the watchapp: proof it is installed (setup checklist, #28). */
    private val onWatchappReached: () -> Unit = {},
    /**
     * The newer phone app version while an update is available, else null; read on every state
     * send so the watch can show the update notice (REQ-ANDROID-017, #94).
     */
    private val latestPhoneUpdate: () -> String? = { null },
    /**
     * Called with the watchapp version from a WATCH_READY the watch really sent (key 11), so a newer
     * watchapp can reveal a phone app update (REQ-ANDROID-019). The readiness this controller makes
     * up itself, and the one from `onAppOpened`, carry no version and never call it.
     */
    private val onWatchAppVersion: (String) -> Unit = {},
    /** The mirror of the watchapp's own settings and the phone edits waiting for it (#102). */
    private val watchSettings: WatchSettingsStore = InMemoryWatchSettingsStore(),
    /** Where speed estimates and hide/show decisions are logged for the diagnostics export (#132 FR14). */
    private val drivingLog: DrivingLogRepository? = null,
    /** Reads the travel speed from navigation notifications (#132 FR5). */
    private val speedEstimator: DrivingSpeedEstimator = DrivingSpeedEstimator(),
    /** How often the hidden state looks at the speed when no notification arrives (#132 FR9). */
    private val drivingTickMillis: Long = DRIVING_TICK_MILLIS,
) {
    private val mutex = Mutex()
    private var state = ReducerState(settings = initialSettings)

    /** The reducer's auto-launch flag, readable outside [mutex] (#132 FR9.1). */
    @Volatile
    private var autoLaunch: Boolean = initialSettings.autoLaunchOnSessionStart

    /** Serializes the When driving logic (#132); never held while waiting for [mutex] or [settingsMutex]. */
    private val drivingMutex = Mutex()
    private var driving = DrivingState()
    private var drivingTicker: Job? = null
    private var lastMeasuredKmh: Double? = null

    /** Hidden while driving: no state goes to the watch, so nothing relaunches it (#132 FR8.1). */
    @Volatile
    private var hiddenForDriving = false

    /** When this controller last launched the watchapp itself, to tell a manual open apart (FR10). */
    @Volatile
    private var lastOwnLaunchMillis: Long? = null

    private val _drivingStatus = MutableStateFlow(DrivingStatus())

    /**
     * Whether the watchapp is hidden while driving, with the last measured speed, for the
     * dashboard (#132 FR13). While [DrivingStatus.hidden] is true nothing may bring the watchapp to
     * the front; a future "keep the watchapp in front" behaviour (#127) must check it (FR8.4).
     */
    val drivingStatus: StateFlow<DrivingStatus> = _drivingStatus.asStateFlow()

    private val _status = MutableStateFlow(WatchLinkStatus())

    /** What the watch was last sent, for the dashboard (#28); updated after every reduction. */
    val status: StateFlow<WatchLinkStatus> = _status.asStateFlow()

    /** Serializes the settings sync, which runs beside the reducer rather than inside it. */
    private val settingsMutex = Mutex()

    private val _watchappOpen = MutableStateFlow(false)

    /**
     * Whether phone edits of the watch settings can go to the watchapp now: it sent its settings in
     * this session and has not closed or failed a send since (REQ-ANDROID-021).
     */
    val watchappOpen: StateFlow<Boolean> = _watchappOpen.asStateFlow()

    /**
     * Begin collecting inbound watch messages. A transport failure (no Pebble app installed, a
     * receiver the platform rejects, …) must degrade the watch link, not take the process down with
     * it, so the stream is caught rather than left to reach the scope's uncaught handler.
     */
    fun start() {
        scope.launch {
            transport.inbound
                .catch { Timber.e(it, "Watch transport failed; inbound watch messages are disabled") }
                .collect { handleInbound(it) }
        }
        scope.launch {
            transport.watchappClosed
                .catch { Timber.e(it, "Watch transport failed; watchapp closes are not seen") }
                .collect { onWatchappClosed() }
        }
    }

    /**
     * Restore the last cached navigation state (REQ-ANDROID-010). Safe to call once on startup
     * before [start]; watch readiness is not restored, so the watch re-handshakes. Settings are
     * preserved from current runtime configuration.
     */
    suspend fun restore() {
        val restored = stateStore?.loadReducerState() ?: return
        mutex.withLock { state = restored.copy(settings = state.settings) }
    }

    suspend fun onInstruction(instruction: NavigationInstruction) =
        dispatch(ReducerEvent.InstructionReceived(instruction, nowSeconds()))

    /**
     * Navigation stopped. While hidden for driving the Stopped state is not sent (it would relaunch
     * the watchapp only to close it again), and the hidden and kept-open states end (#132 FR11).
     */
    suspend fun onNavigationStopped() {
        dispatch(ReducerEvent.NavigationStopped(nowSeconds()))
        resetDriving()
    }

    suspend fun onSettingsChanged(settings: WatchSettings) {
        dispatch(ReducerEvent.SettingsChanged(settings))
        reevaluateWhileHidden()
    }

    suspend fun onConnectionLost() = dispatch(ReducerEvent.ConnectionLost)

    /**
     * The speed fields of a navigation notification (#132 FR5): estimate the speed, log it, and
     * decide whether to hide or show the watchapp. Notifications of apps without speed are ignored.
     */
    suspend fun onSpeedSample(sample: SpeedSample) = drivingMutex.withLock {
        val now = clock.nowMillis()
        val estimate = speedEstimator.add(sample, now) ?: return@withLock
        val config = drivingConfig()
        if (!estimate.isEmpty) logDriving(now, estimate, config, DrivingDecision.SAMPLE)
        if (_status.value.navigating) decideDriving(now, estimate, config)
    }

    private suspend fun handleInbound(message: AppMessage) {
        val event = ProtocolCodec.decodeInbound(message, nowSeconds())
        Timber.i("Watch inbound message decoded to %s", event ?: "no-op")
        ProtocolCodec.watchAppVersion(message)?.let { version ->
            runCatching { onWatchAppVersion(version) }
                .onFailure { Timber.w(it, "Watchapp version %s not recorded", version) }
        }
        // An open while hidden for driving is the user's: it keeps the watchapp open (FR10), so the
        // READY that follows sends the current state again.
        if (event is ReducerEvent.WatchReady || event is ReducerEvent.WatchRequestedState) onWatchappOpenSignal()
        event?.let { dispatch(it) }
        val readyMinor = ProtocolCodec.realWatchReadyMinor(message)
        // The Watch settings screen offers When driving only to a 1.4 watchapp (#132 FR4).
        readyMinor?.let { minor ->
            watchSettings.update { if (it.watchProtocolMinor == minor) it else it.copy(watchProtocolMinor = minor) }
        }
        val watchSettingsSent = ProtocolCodec.decodeWatchSettings(message)
        if (watchSettingsSent != null) {
            onWatchSettings(watchSettingsSent)
        } else {
            readyMinor
                ?.takeIf { it < SETTINGS_PROTOCOL_MINOR }
                ?.let { onOutdatedWatchapp(it) }
        }
    }

    /**
     * A setting edited on the phone's Watch settings screen (REQ-ANDROID-021): it goes into the
     * mirror and to the watchapp if it is open, else it waits for the next time it opens.
     */
    suspend fun onPhoneSettingEdit(setting: WatchSetting, value: Int) {
        val compactBefore = compactDirections()
        settingsMutex.withLock {
            watchSettings.update { WatchSettingsSync.onPhoneEdit(it, setting, value) }
            sendPendingSettings()
        }
        if (compactDirections() != compactBefore) resendForDirectionsText()
        // "Show directions" picked while hidden shows the watchapp at once (#132 FR12).
        reevaluateWhileHidden()
    }

    /** The watchapp closed on the watch: edits wait for it to open again. */
    fun onWatchappClosed() {
        _watchappOpen.value = false
    }

    private suspend fun onWatchSettings(incoming: WatchSettingsModel) {
        val compactBefore = compactDirections()
        settingsMutex.withLock {
            // It just sent this, so it is open; queued edits that are newer go now, without a relaunch.
            _watchappOpen.value = true
            var decision: WatchSettingsSync.Decision? = null
            watchSettings.update { current ->
                WatchSettingsSync.onWatchSettings(current, incoming).also { decision = it.decision }.state
            }
            Timber.i("Watch settings revision %d: %s", incoming.revision, decision)
            sendPendingSettings()
        }
        // Directions text changed on the watch (#134 FR7.3).
        if (compactDirections() != compactBefore) resendForDirectionsText()
    }

    /** The Directions text in effect: Compact sends the street alone when there is one (#134 FR7.1). */
    private fun compactDirections(): Boolean = watchSettings.state.value.mirror?.compactDirections == true

    /**
     * Directions text changed (#134 FR7.3): send the current instruction again in the new form, once,
     * without waiting for the next notification. Nothing goes out when no navigation is active, to a
     * watch that is not ready, or while hidden for driving (the state send skips it, #132 FR8.1).
     */
    private suspend fun resendForDirectionsText() {
        val navigating = mutex.withLock { state.current is NavigationState.Navigating }
        if (!navigating || hiddenForDriving) return
        Timber.i("Directions text changed: sending the current instruction again")
        dispatch(ReducerEvent.WatchRequestedState(nowSeconds()))
    }

    private fun onOutdatedWatchapp(minor: Int) {
        Timber.i("Watchapp protocol 1.%d sends no settings; the Watch settings screen asks for an update", minor)
        watchSettings.update { it.copy(watchappOutdated = true) }
    }

    /** Send the pending edits, only while the watchapp is open; a failed send waits for it to reopen. */
    private suspend fun sendPendingSettings() {
        if (!_watchappOpen.value) return
        val (revision, values) = WatchSettingsSync.outgoing(watchSettings.state.value) ?: return
        if (transport.sendToOpenApp(ProtocolCodec.encodeSettings(revision, values)) != SendResult.SENT) {
            Timber.w("Watch settings not delivered; they wait for the watchapp to open again")
            _watchappOpen.value = false
        }
    }

    private suspend fun dispatch(event: ReducerEvent) = mutex.withLock {
        val result = NavigationSessionReducer.reduce(state, event)
        state = result.state
        autoLaunch = state.settings.autoLaunchOnSessionStart
        _status.value = WatchLinkStatus(
            navigating = state.current is NavigationState.Navigating,
            lastSent = state.lastSentInstruction,
        )
        for (effect in result.effects) {
            runEffect(effect)
        }
        // Persist the latest state so it can be restored after process death (no queue is kept).
        stateStore?.let { store ->
            val snapshot = state
            scope.launch { store.save(snapshot) }
        }
    }

    private suspend fun runEffect(effect: ReducerEffect) {
        when (effect) {
            ReducerEffect.LaunchWatchApp -> {
                Timber.i("Effect: launch watchapp")
                lastOwnLaunchMillis = clock.nowMillis()
                transport.launchApp()
                scheduleAutonomousReady()
            }
            // While hidden for driving, a send would relaunch the watchapp (send() does on
            // FailedDifferentAppOpen). The reducer keeps the state; the show sends it (#132 FR8.1).
            is ReducerEffect.SendState -> if (hiddenForDriving) {
                Timber.i("Hidden while driving: state %s not sent", effect.state)
            } else {
                Timber.i("Effect: send state %s to watch", effect.state)
                sendWithRetry(
                    ProtocolCodec.encodeState(
                        effect.state, effect.flags, appVersion, latestPhoneUpdate(), compact = compactDirections(),
                    ),
                )
            }
            is ReducerEffect.SendCompatibilityError -> if (hiddenForDriving) {
                Timber.i("Hidden while driving: compatibility error %d not sent", effect.errorCode)
            } else {
                Timber.i("Effect: send compatibility error %d", effect.errorCode)
                transport.send(ProtocolCodec.encodeCompatibilityError(effect.errorCode))
            }
        }
    }

    /**
     * The watch announces itself with WATCH_READY once it has opened AppMessage, and normally that
     * handshake is what unblocks the first state send. The Core Devices Pebble app forwards it to
     * us once the watchapp declares `companionApp` in its package.json (confirmed on-device in
     * v0.0.74). Before that declaration it did not, and the watch sat on "Connecting" forever. This
     * synthesized readiness stays as a fallback for any companion or moment where WATCH_READY does
     * not reach us: since we have just launched our app on a connected watch, we synthesize
     * readiness ourselves after a short settle delay that lets the watchapp finish opening its
     * inbox. A genuine WATCH_READY is still handled and simply re-sends the current state.
     */
    private fun scheduleAutonomousReady() {
        scope.launch {
            delay(readySettleMillis)
            dispatch(ReducerEvent.WatchReady(Protocol.MAJOR, Protocol.MINOR, nowSeconds()))
        }
    }

    private suspend fun sendWithRetry(message: AppMessage) {
        var attempt = 0
        while (attempt < maxSendAttempts) {
            if (transport.send(message) == SendResult.SENT) {
                onWatchappReached()
                return
            }
            attempt++
            if (attempt < maxSendAttempts) delay(baseBackoffMillis shl attempt)
        }
    }

    private fun nowSeconds(): Long = clock.nowMillis() / 1000

    // --- When driving (#132) ---------------------------------------------------------------------

    /** The When driving settings as they are now; off for a watchapp without them (FR4, NFR5). */
    private fun drivingConfig(): DrivingConfig {
        val sync = watchSettings.state.value
        return DrivingConfig.from(sync.mirror, sync.hasDrivingSettings, autoLaunch)
    }

    /** Run the policy and act on its decision. Called with [drivingMutex] held. */
    private suspend fun decideDriving(now: Long, estimate: SpeedEstimate, config: DrivingConfig) {
        estimate.measuredKmh?.let { lastMeasuredKmh = it }
        val result = DrivingPolicy.evaluate(driving, now, estimate, config)
        driving = result.state
        when (result.action) {
            DrivingAction.NONE -> Unit
            DrivingAction.HIDE -> {
                // The flag first, so no state send can slip through and relaunch it (FR8.1).
                hiddenForDriving = true
                logDriving(now, estimate, config, DrivingDecision.HIDE)
                Timber.i("Driving at about %.0f km/h: closing the watchapp", estimate.measuredKmh ?: 0.0)
                startDrivingTicker()
                transport.closeApp()
                _watchappOpen.value = false
            }
            DrivingAction.SHOW -> {
                hiddenForDriving = false
                logDriving(now, estimate, config, DrivingDecision.SHOW)
                Timber.i("No longer driving: reopening the watchapp")
                // The one relaunch REQ-WATCH-006 allows: the launch path sends the current state (FR9).
                lastOwnLaunchMillis = now
                transport.launchApp()
                scheduleAutonomousReady()
            }
            DrivingAction.LEAVE_HIDDEN -> {
                // Auto-launch is off: never reopen it; updates resume when the user opens it (FR9.1).
                hiddenForDriving = false
                logDriving(now, estimate, config, DrivingDecision.SHOW)
                Timber.i("No longer driving: the watchapp stays closed until it is opened (auto-launch off)")
            }
        }
        // Last, after every suspension: the ticker may be the coroutine running this.
        if (!driving.hidden) stopDrivingTicker()
        publishDrivingStatus()
    }

    /**
     * While hidden, look at the speed every [drivingTickMillis] even without new notifications, so
     * slowing down shows the watchapp again when Google Maps posts less often (FR9). No timer runs
     * while visible.
     */
    private fun startDrivingTicker() {
        drivingTicker?.cancel()
        drivingTicker = scope.launch {
            while (true) {
                delay(drivingTickMillis)
                reevaluateWhileHidden()
            }
        }
    }

    private fun stopDrivingTicker() {
        drivingTicker?.cancel()
        drivingTicker = null
    }

    /** Decide again while hidden: on a tick, or after a settings change (FR9, FR12). */
    private suspend fun reevaluateWhileHidden() = drivingMutex.withLock {
        if (!driving.hidden) return@withLock
        val now = clock.nowMillis()
        decideDriving(now, speedEstimator.estimate(now), drivingConfig())
    }

    /**
     * The watchapp opened (a READY or a state request). While hidden, and not within
     * [MANUAL_OPEN_GRACE_MILLIS] of this controller's own launch, that is the user (FR10).
     */
    private suspend fun onWatchappOpenSignal() = drivingMutex.withLock {
        if (!driving.hidden) return@withLock
        val now = clock.nowMillis()
        val launched = lastOwnLaunchMillis
        if (launched != null && now - launched < MANUAL_OPEN_GRACE_MILLIS) return@withLock
        driving = DrivingPolicy.onUserOpened(driving)
        hiddenForDriving = false
        logDriving(now, speedEstimator.estimate(now), drivingConfig(), DrivingDecision.KEPT_OPEN)
        Timber.i("Watchapp opened while hidden for driving: it stays open for this navigation")
        stopDrivingTicker()
        publishDrivingStatus()
    }

    /** Navigation stopped: the hidden and kept-open states end and the next session starts fresh (FR11). */
    private suspend fun resetDriving() = drivingMutex.withLock {
        if (driving.hidden || driving.keptOpen) {
            val now = clock.nowMillis()
            logDriving(now, null, drivingConfig(), DrivingDecision.RESET)
        }
        driving = DrivingPolicy.onSessionStopped()
        hiddenForDriving = false
        lastMeasuredKmh = null
        speedEstimator.reset()
        stopDrivingTicker()
        publishDrivingStatus()
    }

    private fun logDriving(now: Long, estimate: SpeedEstimate?, config: DrivingConfig, decision: DrivingDecision) {
        drivingLog?.add(
            DrivingLogEntry(
                timeMillis = now,
                measuredKmh = estimate?.measuredKmh,
                plannedKmh = estimate?.plannedKmh,
                thresholdKmh = config.thresholdKmh,
                decision = decision,
            ),
        )
    }

    private fun publishDrivingStatus() {
        _drivingStatus.value = DrivingStatus(hidden = driving.hidden, measuredKmh = lastMeasuredKmh)
    }

    private companion object {
        /** The protocol minor from which a watchapp sends its settings (protocol 1.3). */
        const val SETTINGS_PROTOCOL_MINOR = 3

        /** While hidden, the speed is looked at this often without new notifications (#132). */
        const val DRIVING_TICK_MILLIS = 10_000L

        /**
         * An open this soon after the controller's own launch is that launch (its autonomous READY
         * comes 1.5 s later), not the user (#132 FR10).
         */
        const val MANUAL_OPEN_GRACE_MILLIS = 5_000L
    }
}

/**
 * The dashboard's view of When driving (#132 FR13): whether the watchapp is hidden while driving,
 * and the last measured speed in km/h (null before one was known).
 */
data class DrivingStatus(
    val hidden: Boolean = false,
    val measuredKmh: Double? = null,
)

/**
 * The dashboard's view of the watch link (#28): whether navigation is active and the last
 * instruction handed to the watch. "Sent" means the phone sent it; delivery itself is best-effort.
 */
data class WatchLinkStatus(
    val navigating: Boolean = false,
    val lastSent: NavigationInstruction? = null,
)
