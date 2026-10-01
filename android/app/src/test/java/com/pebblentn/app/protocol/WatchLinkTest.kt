package com.pebblentn.app.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

/** #28: the dashboard's watch line. */
class WatchLinkTest {

    @Test
    fun noCompanionWinsOverEverythingElse() {
        assertEquals(WatchLink.NoCompanion, WatchLink.from(companionPresent = false, connectedNames = listOf("Pebble Time 2")))
    }

    @Test
    fun aCompanionWithoutWatchesIsNotConnected() {
        assertEquals(WatchLink.NotConnected, WatchLink.from(companionPresent = true, connectedNames = emptyList()))
    }

    @Test
    fun connectedWatchesAreNamed() {
        assertEquals(WatchLink.Connected(listOf("Pebble Time 2")), WatchLink.from(true, listOf("Pebble Time 2")))
    }
}
