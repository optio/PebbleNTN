package com.pebblentn.app.protocol

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Test double for [WatchTransport]. Records launches and sent messages, lets a test push inbound
 * messages and watchapp closes, and can be scripted to fail sends. No Android or Pebble dependency.
 */
open class FakeWatchTransport(
    /** Results returned by successive [send] calls; the last entry repeats once exhausted. */
    private val sendResults: List<SendResult> = listOf(SendResult.SENT),
) : WatchTransport {

    private val inboundFlow = MutableSharedFlow<AppMessage>(extraBufferCapacity = 64)
    override val inbound: Flow<AppMessage> = inboundFlow

    private val closedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    override val watchappClosed: Flow<Unit> = closedFlow

    var launchCount: Int = 0
        private set

    val sent: MutableList<AppMessage> = mutableListOf()

    /** The messages sent with [sendToOpenApp], which never relaunches the watchapp; also in [sent]. */
    val sentToOpenApp: MutableList<AppMessage> = mutableListOf()

    private var sendIndex = 0

    override suspend fun launchApp() {
        launchCount++
    }

    override suspend fun send(message: AppMessage): SendResult {
        sent += message
        val result = sendResults[sendIndex.coerceAtMost(sendResults.lastIndex)]
        sendIndex++
        return result
    }

    override suspend fun sendToOpenApp(message: AppMessage): SendResult {
        sentToOpenApp += message
        return send(message)
    }

    /** Simulate an AppMessage arriving from the watch. */
    suspend fun emitInbound(message: AppMessage) {
        inboundFlow.emit(message)
    }

    /** Simulate the companion reporting that the watchapp was closed. */
    suspend fun emitClosed() {
        closedFlow.emit(Unit)
    }
}
