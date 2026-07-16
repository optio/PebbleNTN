package com.pebblentn.app.protocol

import kotlinx.coroutines.flow.Flow

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
}

/** Outcome of a single send attempt. Retry/backoff policy lives above the transport. */
enum class SendResult { SENT, FAILED }
