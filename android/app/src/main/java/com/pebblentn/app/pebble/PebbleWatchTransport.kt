package com.pebblentn.app.pebble

import android.content.Context
import com.pebblentn.app.protocol.AppMessage
import com.pebblentn.app.protocol.SendResult
import com.pebblentn.app.protocol.WatchTransport
import io.rebble.pebblekit2.client.DefaultPebbleSender
import kotlinx.coroutines.flow.Flow
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
 * Not exercisable by unit tests (the sender talks to a bound Pebble app); verified by compilation
 * plus on-device end-to-end testing.
 */
class PebbleWatchTransport(
    context: Context,
    private val appUuid: UUID = PEBBLENTN_UUID,
) : WatchTransport {

    private val sender = DefaultPebbleSender(context.applicationContext)

    override val inbound: Flow<AppMessage> = WatchInboundBus.messages

    override suspend fun launchApp() {
        runCatching { sender.startAppOnTheWatch(appUuid) }
    }

    override suspend fun send(message: AppMessage): SendResult =
        runCatching { sender.sendDataToPebble(appUuid, PebbleAppMessageMapper.toDictionary(message)) }
            .fold(onSuccess = { SendResult.SENT }, onFailure = { SendResult.FAILED })

    companion object {
        /** Must match the watchapp UUID in watchapp/package.json. */
        val PEBBLENTN_UUID: UUID = UUID.fromString("9c7cef54-517e-42c8-8e95-0a8316e70cf1")
    }
}
