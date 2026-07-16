package com.pebblentn.app.pebble

import com.pebblentn.app.protocol.AppMessage
import com.pebblentn.app.protocol.Protocol
import io.rebble.pebblekit2.client.BasePebbleListenerService
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import timber.log.Timber
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

    /**
     * PebbleKit lifecycle callback: our watchapp came to the foreground on the watch. The Core
     * Devices companion forwards this even though it does not forward the watch's WATCH_READY
     * AppMessage, so it is a reliable "the watch is ready for state" signal. We turn it into a
     * synthetic WATCH_READY so the controller (re)sends the current navigation state.
     */
    override fun onAppOpened(watchappUUID: UUID, watch: WatchIdentifier) {
        if (watchappUUID != PebbleWatchTransport.PEBBLENTN_UUID) return
        Timber.i("PebbleKit: watchapp opened on the watch; requesting state (re)send")
        WatchInboundBus.emit(
            AppMessage.builder()
                .putInt(Protocol.Keys.EVENT, Protocol.Events.WATCH_READY)
                .putInt(Protocol.Keys.PROTOCOL_MAJOR, Protocol.MAJOR)
                .putInt(Protocol.Keys.PROTOCOL_MINOR, Protocol.MINOR)
                .build(),
        )
    }
}
