package com.pebblentn.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.core.WatchPlatform
import com.pebblentn.app.core.WatchSetting
import com.pebblentn.app.core.WatchSettingsSync
import com.pebblentn.app.core.WatchSettingsSyncState
import com.pebblentn.app.protocol.WatchMessages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The mirror of the watch settings survives a restart, pending edits included (REQ-ANDROID-021). */
@RunWith(RobolectricTestRunner::class)
class WatchSettingsMirrorRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun emptyUntilAWatchappSendsItsSettings() {
        val repo = WatchSettingsMirrorRepository(context)
        assertNull(repo.state.value.mirror)
        assertFalse(repo.state.value.receivedFromWatch)
        assertTrue(repo.state.value.pending.isEmpty())
    }

    @Test
    fun theMirrorRevisionAndPendingEditsSurviveANewInstance() {
        val first = WatchSettingsMirrorRepository(context)
        val fromWatch = WatchMessages.settings(
            revision = 98,
            overrides = mapOf(WatchSetting.ACCENT to 6, WatchSetting.TOUCH to 1),
        )
        first.update { WatchSettingsSync.onWatchSettings(it, fromWatch).state }
        first.update { WatchSettingsSync.onPhoneEdit(it, WatchSetting.UNITS, 1) }
        first.update { WatchSettingsSync.onPhoneEdit(it, WatchSetting.INVERT, 1) }

        val restarted = WatchSettingsMirrorRepository(context).state.value
        assertEquals(first.state.value, restarted)
        val mirror = restarted.mirror!!
        assertEquals("98, then two edits wrap to 0", 0, mirror.revision)
        assertEquals(6, mirror.accent)
        assertEquals(1, mirror.units)
        assertTrue(mirror.inverted)
        assertTrue(mirror.touchEnabled)
        assertEquals(WatchPlatform.EMERY, mirror.platform)
        assertEquals(WatchMessages.EMERY_CAPS, mirror.capabilities)
        assertEquals(setOf(WatchSetting.UNITS, WatchSetting.INVERT), restarted.pending)
        assertTrue(restarted.receivedFromWatch)
    }

    @Test
    fun theOutdatedFlagSurvivesANewInstance() {
        WatchSettingsMirrorRepository(context).update { WatchSettingsSyncState(watchappOutdated = true) }
        assertTrue(WatchSettingsMirrorRepository(context).state.value.watchappOutdated)
    }
}
