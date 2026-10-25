package com.pebblentn.app.pebble

import com.pebblentn.app.core.EpochClock
import com.pebblentn.app.core.NavigationInstruction
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
) {
    private val mutex = Mutex()
    private var state = ReducerState(settings = initialSettings)

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

    suspend fun onNavigationStopped() = dispatch(ReducerEvent.NavigationStopped(nowSeconds()))

    suspend fun onSettingsChanged(settings: WatchSettings) = dispatch(ReducerEvent.SettingsChanged(settings))

    suspend fun onConnectionLost() = dispatch(ReducerEvent.ConnectionLost)

    private suspend fun handleInbound(message: AppMessage) {
        val event = ProtocolCodec.decodeInbound(message, nowSeconds())
        Timber.i("Watch inbound message decoded to %s", event ?: "no-op")
        ProtocolCodec.watchAppVersion(message)?.let { version ->
            runCatching { onWatchAppVersion(version) }
                .onFailure { Timber.w(it, "Watchapp version %s not recorded", version) }
        }
        event?.let { dispatch(it) }
        val watchSettingsSent = ProtocolCodec.decodeWatchSettings(message)
        if (watchSettingsSent != null) {
            onWatchSettings(watchSettingsSent)
        } else {
            ProtocolCodec.realWatchReadyMinor(message)
                ?.takeIf { it < SETTINGS_PROTOCOL_MINOR }
                ?.let { onOutdatedWatchapp(it) }
        }
    }

    /**
     * A setting edited on the phone's Watch settings screen (REQ-ANDROID-021): it goes into the
     * mirror and to the watchapp if it is open, else it waits for the next time it opens.
     */
    suspend fun onPhoneSettingEdit(setting: WatchSetting, value: Int) = settingsMutex.withLock {
        watchSettings.update { WatchSettingsSync.onPhoneEdit(it, setting, value) }
        sendPendingSettings()
    }

    /** The watchapp closed on the watch: edits wait for it to open again. */
    fun onWatchappClosed() {
        _watchappOpen.value = false
    }

    private suspend fun onWatchSettings(incoming: WatchSettingsModel) = settingsMutex.withLock {
        // It just sent this, so it is open; queued edits that are newer go now, without a relaunch.
        _watchappOpen.value = true
        var decision: WatchSettingsSync.Decision? = null
        watchSettings.update { current ->
            WatchSettingsSync.onWatchSettings(current, incoming).also { decision = it.decision }.state
        }
        Timber.i("Watch settings revision %d: %s", incoming.revision, decision)
        sendPendingSettings()
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
                transport.launchApp()
                scheduleAutonomousReady()
            }
            is ReducerEffect.SendState -> {
                Timber.i("Effect: send state %s to watch", effect.state)
                sendWithRetry(ProtocolCodec.encodeState(effect.state, effect.flags, appVersion, latestPhoneUpdate()))
            }
            is ReducerEffect.SendCompatibilityError -> {
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

    private companion object {
        /** The protocol minor from which a watchapp sends its settings (protocol 1.3). */
        const val SETTINGS_PROTOCOL_MINOR = 3
    }
}

/**
 * The dashboard's view of the watch link (#28): whether navigation is active and the last
 * instruction handed to the watch. "Sent" means the phone sent it; delivery itself is best-effort.
 */
data class WatchLinkStatus(
    val navigating: Boolean = false,
    val lastSent: NavigationInstruction? = null,
)
