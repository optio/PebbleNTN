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
import com.pebblentn.app.protocol.ProtocolCodec
import com.pebblentn.app.protocol.SendResult
import com.pebblentn.app.protocol.WatchTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
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
    private val stateStore: NavigationStateRepository? = null,
) {
    private val mutex = Mutex()
    private var state = ReducerState()

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
     * before [start]; watch readiness is not restored, so the watch re-handshakes.
     */
    suspend fun restore() {
        val restored = stateStore?.loadReducerState() ?: return
        mutex.withLock { state = restored }
    }

    suspend fun onInstruction(instruction: NavigationInstruction) =
        dispatch(ReducerEvent.InstructionReceived(instruction, nowSeconds()))

    suspend fun onNavigationStopped() = dispatch(ReducerEvent.NavigationStopped(nowSeconds()))

    suspend fun onSettingsChanged(settings: WatchSettings) = dispatch(ReducerEvent.SettingsChanged(settings))

    suspend fun onConnectionLost() = dispatch(ReducerEvent.ConnectionLost)

    private suspend fun handleInbound(message: AppMessage) {
        ProtocolCodec.decodeInbound(message, nowSeconds())?.let { dispatch(it) }
    }

    private suspend fun dispatch(event: ReducerEvent) = mutex.withLock {
        val result = NavigationSessionReducer.reduce(state, event)
        state = result.state
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
            ReducerEffect.LaunchWatchApp -> transport.launchApp()
            is ReducerEffect.SendState ->
                sendWithRetry(ProtocolCodec.encodeState(effect.state, effect.flags, appVersion))
            is ReducerEffect.SendCompatibilityError ->
                transport.send(ProtocolCodec.encodeCompatibilityError(effect.errorCode))
        }
    }

    private suspend fun sendWithRetry(message: AppMessage) {
        var attempt = 0
        while (attempt < maxSendAttempts) {
            if (transport.send(message) == SendResult.SENT) return
            attempt++
            if (attempt < maxSendAttempts) delay(baseBackoffMillis shl attempt)
        }
    }

    private fun nowSeconds(): Long = clock.nowMillis() / 1000
}
