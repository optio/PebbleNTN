package com.pebblentn.app.pebble

import com.pebblentn.app.core.EpochClock
import com.pebblentn.app.core.NavigationInstruction
import com.pebblentn.app.core.NavigationSessionReducer
import com.pebblentn.app.core.ReducerEffect
import com.pebblentn.app.core.ReducerEvent
import com.pebblentn.app.core.ReducerState
import com.pebblentn.app.core.WatchSettings
import com.pebblentn.app.data.NavigationStateRepository
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
) {
    private val mutex = Mutex()
    private var state = ReducerState(settings = initialSettings)

    private val _status = MutableStateFlow(WatchLinkStatus())

    /** What the watch was last sent, for the dashboard (#28); updated after every reduction. */
    val status: StateFlow<WatchLinkStatus> = _status.asStateFlow()

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
}

/**
 * The dashboard's view of the watch link (#28): whether navigation is active and the last
 * instruction handed to the watch. "Sent" means the phone sent it; delivery itself is best-effort.
 */
data class WatchLinkStatus(
    val navigating: Boolean = false,
    val lastSent: NavigationInstruction? = null,
)
