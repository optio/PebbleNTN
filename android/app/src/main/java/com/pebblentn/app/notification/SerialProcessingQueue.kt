package com.pebblentn.app.notification

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * A single-consumer work queue that serializes notification processing off the listener callback
 * thread (REQ-ANDROID-006). [enqueue] never blocks the caller; the work runs sequentially on the
 * provided [scope]'s dispatcher, preserving submission order.
 */
class SerialProcessingQueue(scope: CoroutineScope) {

    private val channel = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (work in channel) {
                work()
            }
        }
    }

    /** Submit work to run after all previously enqueued work, without blocking the caller. */
    fun enqueue(work: suspend () -> Unit) {
        // UNLIMITED capacity: trySend always succeeds and never blocks the callback thread.
        channel.trySend(work)
    }
}
