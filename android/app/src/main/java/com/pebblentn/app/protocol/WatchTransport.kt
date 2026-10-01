package com.pebblentn.app.protocol

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Abstraction over the Pebble link. Isolating the watch behind this interface keeps the reducer and
 * codec free of SDK types (spec/000-overview.md: "Watch transport isolated behind an interface").
 * The production [PebbleWatchTransport] arrives in M7; M1 ships this interface and a fake.
 */
interface WatchTransport {

    /** AppMessages received from the watch (e.g. WATCH_READY, WATCH_REQUEST_STATE). */
    val inbound: Flow<AppMessage>

    /** Launch the watchapp on the connected Pebble. */
    suspend fun launchApp()

    /** Send one AppMessage to the watch. Delivery is best-effort; see [SendResult]. */
    suspend fun send(message: AppMessage): SendResult

    /**
     * One-line, human-readable description of the current link (selected companion app, connected
     * watches, …) for a startup log line. Used only for diagnostics; the default says nothing so
     * fakes need not implement it.
     */
    suspend fun linkDiagnostics(): String = "diagnostics unavailable"

    /**
     * The watch link as the dashboard shows it (#28), re-emitting when watches connect or
     * disconnect. Fakes default to [WatchLink.Unknown].
     */
    fun watchLink(): Flow<WatchLink> = flowOf(WatchLink.Unknown)
}

/** Whether a Pebble is reachable, for the dashboard (#28). */
sealed interface WatchLink {
    /** Not determined yet. */
    data object Unknown : WatchLink

    /** No Pebble companion app (the Core Devices / Rebble Pebble app) is installed or selected. */
    data object NoCompanion : WatchLink

    /** The companion is there but no watch is connected to it. */
    data object NotConnected : WatchLink

    data class Connected(val watchNames: List<String>) : WatchLink

    companion object {
        /** Map what the companion reports to a [WatchLink]. */
        fun from(companionPresent: Boolean, connectedNames: List<String>): WatchLink = when {
            !companionPresent -> NoCompanion
            connectedNames.isEmpty() -> NotConnected
            else -> Connected(connectedNames)
        }
    }
}

/** Outcome of a single send attempt. Retry/backoff policy lives above the transport. */
enum class SendResult { SENT, FAILED }
