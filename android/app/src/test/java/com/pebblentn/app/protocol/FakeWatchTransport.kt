package com.pebblentn.app.protocol

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Test double for [WatchTransport]. Records launches and sent messages, lets a test push inbound
 * messages, and can be scripted to fail sends. No Android or Pebble dependency.
 */
class FakeWatchTransport(
    /** Results returned by successive [send] calls; the last entry repeats once exhausted. */
    private val sendResults: List<SendResult> = listOf(SendResult.SENT),
) : WatchTransport {

    private val inboundFlow = MutableSharedFlow<AppMessage>(extraBufferCapacity = 64)
    override val inbound: Flow<AppMessage> = inboundFlow

    var launchCount: Int = 0
        private set

    val sent: MutableList<AppMessage> = mutableListOf()

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

    /** Simulate an AppMessage arriving from the watch. */
    suspend fun emitInbound(message: AppMessage) {
        inboundFlow.emit(message)
    }
}
