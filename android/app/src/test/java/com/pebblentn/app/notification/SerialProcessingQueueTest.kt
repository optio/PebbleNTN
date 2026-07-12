package com.pebblentn.app.notification

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SerialProcessingQueueTest {

    @Test
    fun runsWorkInSubmissionOrder() = runTest {
        val queue = SerialProcessingQueue(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        val order = mutableListOf<Int>()

        repeat(5) { i -> queue.enqueue { order.add(i) } }
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(0, 1, 2, 3, 4), order)
    }

    @Test
    fun processesSequentiallyNotConcurrently() = runTest {
        val queue = SerialProcessingQueue(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        var active = 0
        var maxConcurrent = 0

        repeat(10) {
            queue.enqueue {
                active++
                maxConcurrent = maxOf(maxConcurrent, active)
                active--
            }
        }
        testScheduler.advanceUntilIdle()

        assertEquals(1, maxConcurrent)
    }
}
