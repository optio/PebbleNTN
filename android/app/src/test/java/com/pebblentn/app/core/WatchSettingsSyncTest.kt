package com.pebblentn.app.core

import com.pebblentn.app.core.WatchSettingsSync.Decision
import com.pebblentn.app.protocol.WatchMessages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** The phone's settings revisions and merge rules, mirroring the watch's (REQ-ANDROID-021, #102). */
class WatchSettingsSyncTest {

    @Test
    fun nextWrapsFrom99To0() {
        assertEquals(1, WatchSettingsSync.next(0))
        assertEquals(43, WatchSettingsSync.next(42))
        assertEquals(0, WatchSettingsSync.next(99))
        assertEquals("out of range restarts", 0, WatchSettingsSync.next(100))
    }

    @Test
    fun newerIsOneTo49Ahead() {
        assertTrue(WatchSettingsSync.isNewer(11, 10))
        assertTrue(WatchSettingsSync.isNewer(59, 10))
        assertFalse("equal", WatchSettingsSync.isNewer(10, 10))
        assertFalse("50 apart", WatchSettingsSync.isNewer(60, 10))
        assertFalse("50 apart the other way", WatchSettingsSync.isNewer(10, 60))
        assertFalse("51 ahead is behind", WatchSettingsSync.isNewer(61, 10))
        assertFalse("out of range", WatchSettingsSync.isNewer(100, 10))
    }

    @Test
    fun rolloverZeroIsNewerThan99() {
        assertTrue(WatchSettingsSync.isNewer(0, 99))
        assertTrue(WatchSettingsSync.isNewer(3, 97))
        assertFalse(WatchSettingsSync.isNewer(99, 0))
    }

    @Test
    fun decideLetsThePhoneWinOnlyWithNewerPendingEdits() {
        assertEquals(Decision.KEEP_PHONE, WatchSettingsSync.decide(11, 10, differs = true, hasPending = true))
        assertEquals(Decision.ADOPT_WATCH, WatchSettingsSync.decide(10, 11, differs = true, hasPending = true))
        assertEquals("equal and different: the watch wins", Decision.ADOPT_WATCH, WatchSettingsSync.decide(10, 10, differs = true, hasPending = true))
        assertEquals(Decision.SAME, WatchSettingsSync.decide(10, 10, differs = false, hasPending = true))
        assertEquals("50 apart: the watch wins", Decision.ADOPT_WATCH, WatchSettingsSync.decide(60, 10, differs = true, hasPending = true))
        assertEquals("newer but nothing pending", Decision.ADOPT_WATCH, WatchSettingsSync.decide(11, 10, differs = true, hasPending = false))
    }

    @Test
    fun firstWatchSettingsAreAdopted() {
        val incoming = WatchMessages.settings(revision = 7, overrides = mapOf(WatchSetting.ACCENT to 3))
        val result = WatchSettingsSync.onWatchSettings(WatchSettingsSyncState(), incoming)
        assertEquals(Decision.ADOPT_WATCH, result.decision)
        assertEquals(incoming, result.state.mirror)
        assertTrue(result.state.receivedFromWatch)
    }

    @Test
    fun aPhoneEditBumpsTheRevisionAndIsPending() {
        val state = WatchSettingsSyncState(mirror = WatchMessages.settings(revision = 99), receivedFromWatch = true)
        val edited = WatchSettingsSync.onPhoneEdit(state, WatchSetting.INVERT, 1)
        val mirror = edited.mirror!!
        assertEquals(0, mirror.revision)
        assertTrue(mirror.inverted)
        assertEquals(setOf(WatchSetting.INVERT), edited.pending)
        assertEquals(0 to mapOf(WatchSetting.INVERT to 1), WatchSettingsSync.outgoing(edited))
    }

    @Test
    fun aPhoneEditTheWatchCannotTakeIsIgnored() {
        val bw = WatchMessages.settings(revision = 1, platform = WatchPlatform.APLITE, caps = WatchCapabilities.NONE)
        val state = WatchSettingsSyncState(mirror = bw, receivedFromWatch = true)
        assertSame(state, WatchSettingsSync.onPhoneEdit(state, WatchSetting.ACCENT, 2))
        val nothingKnown = WatchSettingsSyncState()
        assertSame("no watch settings yet", nothingKnown, WatchSettingsSync.onPhoneEdit(nothingKnown, WatchSetting.UNITS, 1))
        assertNull(WatchSettingsSync.outgoing(state))
    }

    @Test
    fun theConfirmationClearsThePendingEdit() {
        val state = WatchSettingsSync.onPhoneEdit(
            WatchSettingsSyncState(mirror = WatchMessages.settings(revision = 4), receivedFromWatch = true),
            WatchSetting.UNITS,
            1,
        )
        val confirmation = WatchMessages.settings(revision = 5, overrides = mapOf(WatchSetting.UNITS to 1))
        val result = WatchSettingsSync.onWatchSettings(state, confirmation)
        assertEquals(Decision.SAME, result.decision)
        assertTrue(result.state.pending.isEmpty())
    }

    @Test
    fun newerWatchSettingsReplaceTheQueuedValuesTheyCarry() {
        val state = WatchSettingsSync.onPhoneEdit(
            WatchSettingsSyncState(mirror = WatchMessages.settings(revision = 4), receivedFromWatch = true),
            WatchSetting.UNITS,
            1,
        )
        val watchNewer = WatchMessages.settings(revision = 7, overrides = mapOf(WatchSetting.ACCENT to 5))
        val result = WatchSettingsSync.onWatchSettings(state, watchNewer)
        assertEquals(Decision.ADOPT_WATCH, result.decision)
        val mirror = result.state.mirror!!
        assertEquals(0, mirror.units)
        assertEquals(5, mirror.accent)
        assertTrue(result.state.pending.isEmpty())
    }

    @Test
    fun olderWatchSettingsKeepThePhoneEditsAndTakeThePlatform() {
        val state = WatchSettingsSync.onPhoneEdit(
            WatchSettingsSyncState(mirror = WatchMessages.settings(revision = 4), receivedFromWatch = true),
            WatchSetting.UNITS,
            1,
        )
        val older = WatchMessages.settings(revision = 4, platform = WatchPlatform.CHALK)
        val result = WatchSettingsSync.onWatchSettings(state, older)
        assertEquals(Decision.KEEP_PHONE, result.decision)
        val mirror = result.state.mirror!!
        assertEquals(1, mirror.units)
        assertEquals(WatchPlatform.CHALK, mirror.platform)
        assertEquals(setOf(WatchSetting.UNITS), result.state.pending)
    }
}
