package com.pebblentn.app.pebble

import com.pebblentn.app.protocol.AppMessage
import io.rebble.pebblekit2.client.BasePebbleListenerService
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

/**
 * Process-wide bridge from the PebbleKit 2 listener [WatchListenerService] (instantiated by the
 * Android framework) to [PebbleWatchTransport.inbound]. A small replayless buffer decouples the
 * service callback thread from the collector; overflow drops the oldest, which is fine because the
 * watch handshake retransmits state on demand.
 */
object WatchInboundBus {
    private val _messages = MutableSharedFlow<AppMessage>(extraBufferCapacity = 32)
    val messages: Flow<AppMessage> = _messages.asSharedFlow()

    fun emit(message: AppMessage) {
        _messages.tryEmit(message)
    }
}

/**
 * Receives AppMessages from the watchapp over PebbleKit Android 2 (declared in AndroidManifest with
 * the `io.rebble.pebblekit2.RECEIVE_DATA_FROM_WATCH` action). Only messages for our watchapp UUID
 * are forwarded; everything else is acknowledged and ignored. The watchapp filters by UUID too, but
 * this keeps foreign traffic out of our pipeline.
 */
class WatchListenerService : BasePebbleListenerService() {

    override suspend fun onMessageReceived(
        watchappUUID: UUID,
        data: PebbleDictionary,
        watch: WatchIdentifier,
    ): ReceiveResult {
        if (watchappUUID == PebbleWatchTransport.PEBBLENTN_UUID) {
            WatchInboundBus.emit(PebbleAppMessageMapper.fromDictionary(data))
        }
        return ReceiveResult.Ack
    }
}
