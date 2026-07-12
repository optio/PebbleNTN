package com.pebblentn.app.core

/** Injectable wall-clock source (epoch milliseconds), so time-dependent code is testable. */
fun interface EpochClock {
    fun nowMillis(): Long

    companion object {
        val SYSTEM = EpochClock { System.currentTimeMillis() }
    }
}
