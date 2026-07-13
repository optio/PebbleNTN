package com.pebblentn.app.pebble

import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.getpebble.android.kit.Constants
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.util.PebbleDictionary
import com.pebblentn.app.protocol.AppMessage
import com.pebblentn.app.protocol.SendResult
import com.pebblentn.app.protocol.WatchTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Production [WatchTransport] over PebbleKit Android. Isolated here so the reducer/codec stay free
 * of the SDK (spec/000-overview.md). Sends are fire-and-forget at the SDK level; [send] reports
 * SENT once dispatched and FAILED on error, and the [com.pebblentn.app.pebble.NavigationController]
 * layer applies bounded retry/backoff.
 *
 * Not exercisable by unit tests (PebbleKit uses static calls + a bound Pebble app); it is verified
 * by compilation here and by on-device end-to-end testing (see docs/IMPLEMENTATION_STATUS.md).
 */
class PebbleWatchTransport(
    private val context: Context,
    private val appUuid: UUID = PEBBLENTN_UUID,
) : WatchTransport {

    override val inbound: Flow<AppMessage> = callbackFlow {
        val receiver = object : PebbleKit.PebbleDataReceiver(appUuid) {
            override fun receiveData(ctx: Context, transactionId: Int, data: PebbleDictionary) {
                PebbleKit.sendAckToPebble(ctx, transactionId)
                trySend(PebbleAppMessageMapper.fromDictionary(data))
            }
        }
        // NOT PebbleKit.registerReceivedDataHandler: it calls registerReceiver() without an export
        // flag, which is a fatal SecurityException from API 34 on. The broadcast originates in the
        // Pebble app, so the receiver must be exported; PebbleDataReceiver drops any message whose
        // UUID is not ours, so a hostile broadcast cannot inject watch messages.
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Constants.INTENT_APP_RECEIVE),
            ContextCompat.RECEIVER_EXPORTED,
        )
        awaitClose { runCatching { context.unregisterReceiver(receiver) } }
    }

    override suspend fun launchApp() {
        withContext(Dispatchers.IO) { PebbleKit.startAppOnPebble(context, appUuid) }
    }

    override suspend fun send(message: AppMessage): SendResult = withContext(Dispatchers.IO) {
        runCatching { PebbleKit.sendDataToPebble(context, appUuid, PebbleAppMessageMapper.toDictionary(message)) }
            .fold(onSuccess = { SendResult.SENT }, onFailure = { SendResult.FAILED })
    }

    companion object {
        /** Must match the watchapp UUID in watchapp/package.json. */
        val PEBBLENTN_UUID: UUID = UUID.fromString("9c7cef54-517e-42c8-8e95-0a8316e70cf1")
    }
}
