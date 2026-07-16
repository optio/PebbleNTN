package com.pebblentn.app.pebble

import android.content.Context
import com.pebblentn.app.protocol.AppMessage
import com.pebblentn.app.protocol.SendResult
import com.pebblentn.app.protocol.WatchTransport
import io.rebble.pebblekit2.client.DefaultPebbleAndroidAppPicker
import io.rebble.pebblekit2.client.DefaultPebbleInfoRetriever
import io.rebble.pebblekit2.client.DefaultPebbleSender
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.TransmissionResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.UUID

/**
 * Production [WatchTransport] over PebbleKit Android 2 (io.rebble.pebblekit2). Isolated here so the
 * reducer/codec stay free of the SDK (spec/000-overview.md).
 *
 * Outbound uses [DefaultPebbleSender] (coroutine-based; no more classic static calls). Inbound
 * arrives on a manifest-declared [WatchListenerService]; it is bridged to [inbound] through
 * [WatchInboundBus], because PebbleKit 2 delivers watch messages to an Android Service rather than a
 * broadcast we register inline. This also retires the classic-PebbleKit Android 14 `registerReceiver`
 * crash workaround (docs/IMPLEMENTATION_STATUS.md).
 *
 * PebbleKit 2 routes through a *companion app* (the modern Rebble / Core Devices Pebble app) that
 * implements the `io.rebble.pebblekit2.SEND_DATA_TO_WATCH` service and the connected-watch content
 * provider. The classic `com.getpebble.android` app does NOT implement this contract, so it is not a
 * valid companion here — [linkDiagnostics] surfaces which companion (if any) was auto-selected.
 *
 * Not exercisable by unit tests (the sender talks to a bound companion app); verified by compilation
 * plus on-device end-to-end testing.
 */
class PebbleWatchTransport(
    context: Context,
    private val appUuid: UUID = PEBBLENTN_UUID,
) : WatchTransport {

    private val appContext = context.applicationContext
    private val sender = DefaultPebbleSender(appContext)
    private val infoRetriever = DefaultPebbleInfoRetriever(appContext)

    override val inbound: Flow<AppMessage> = WatchInboundBus.messages

    override suspend fun launchApp() {
        runCatching { sender.startAppOnTheWatch(appUuid) }
            .onSuccess { results -> Timber.i("PebbleKit: launchApp -> %s", results) }
            .onFailure { Timber.e(it, "PebbleKit: launchApp failed (no companion app bound?)") }
    }

    override suspend fun send(message: AppMessage): SendResult {
        val dict = PebbleAppMessageMapper.toDictionary(message)
        var results = trySend(dict)

        // The companion refuses delivery with FailedDifferentAppOpen until our watchapp is the app
        // in the foreground on the watch — and right after a session starts it often is not yet.
        // Re-launch it and give it time to come to the front, then retry once. This mirrors the
        // working konsumer/pebble-map-android flow; the ~900 ms wait is essential (a short retry
        // races the launch and fails again).
        if (isDifferentAppOpen(results)) {
            Timber.i("PebbleKit: watchapp not in foreground — relaunching then retrying")
            runCatching { sender.startAppOnTheWatch(appUuid) }
            delay(RELAUNCH_DELAY_MS)
            results = trySend(dict)
        }

        val delivered = results != null && results.isNotEmpty() &&
            results.values.all { it is TransmissionResult.Success }
        return if (delivered) {
            Timber.i("PebbleKit: send ok -> %s", results)
            SendResult.SENT
        } else {
            Timber.w("PebbleKit: send reached no watch -> %s", results)
            SendResult.FAILED
        }
    }

    /** One send attempt; null means the call threw (no companion bound / not connected). */
    private suspend fun trySend(dict: PebbleDictionary): Map<WatchIdentifier, TransmissionResult>? =
        runCatching { sender.sendDataToPebble(appUuid, dict) }
            .onFailure { Timber.e(it, "PebbleKit: send threw (no companion app bound / not connected?)") }
            .getOrNull()

    private fun isDifferentAppOpen(results: Map<WatchIdentifier, TransmissionResult>?): Boolean =
        results?.values?.any { it is TransmissionResult.FailedDifferentAppOpen } == true

    override suspend fun linkDiagnostics(): String {
        val picker = DefaultPebbleAndroidAppPicker.getInstance(appContext)
        val selected = runCatching { picker.getCurrentlySelectedApp() }.getOrNull()
        val eligible = runCatching { picker.getAllEligibleApps() }.getOrDefault(emptyList())
        val watches = runCatching {
            withTimeoutOrNull(WATCH_QUERY_TIMEOUT_MS) { infoRetriever.getConnectedWatches().first() }
        }.getOrNull()
        return buildString {
            append("companion=").append(selected ?: "<none selected>")
            append(", eligibleCompanions=")
            append(if (eligible.isEmpty()) "<none installed>" else eligible.joinToString())
            append(", connectedWatches=")
            append(watches?.let { list -> "${list.size} ${list.map { it.name }}" } ?: "<unknown>")
        }
    }

    companion object {
        /** Must match the watchapp UUID in watchapp/package.json. */
        val PEBBLENTN_UUID: UUID = UUID.fromString("9c7cef54-517e-42c8-8e95-0a8316e70cf1")

        private const val WATCH_QUERY_TIMEOUT_MS = 2_000L

        /**
         * How long to wait after re-launching the watchapp before retrying a send that was refused
         * with FailedDifferentAppOpen. The watchapp needs a moment to become the foreground app on
         * the watch before the companion will route a message to it; ~1 s (the working
         * konsumer/pebble-map-android app uses ~900 ms) is enough to bridge each session's launch
         * transition.
         */
        private const val RELAUNCH_DELAY_MS = 1_000L
    }
}
