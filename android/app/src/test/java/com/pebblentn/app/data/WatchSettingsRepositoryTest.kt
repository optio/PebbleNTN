package com.pebblentn.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WatchSettingsRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun enabledByDefault() {
        val repo = WatchSettingsRepository(context)
        assertTrue(repo.isAutoLaunchEnabled())
        assertTrue(repo.autoLaunchEnabled.value)
        assertTrue(repo.settings.value.autoLaunchOnSessionStart)
    }

    @Test
    fun setAutoLaunchIsObservableImmediately() {
        val repo = WatchSettingsRepository(context)

        repo.setAutoLaunch(false)

        assertFalse(repo.isAutoLaunchEnabled())
        assertFalse(repo.autoLaunchEnabled.value)
        assertFalse(repo.settings.value.autoLaunchOnSessionStart)
    }

    @Test
    fun settingSurvivesRestart() {
        val repo1 = WatchSettingsRepository(context)
        repo1.setAutoLaunch(false)

        val repo2 = WatchSettingsRepository(context)
        assertFalse(repo2.isAutoLaunchEnabled())
        assertFalse(repo2.autoLaunchEnabled.value)
        assertFalse(repo2.settings.value.autoLaunchOnSessionStart)

        repo2.setAutoLaunch(true)
        val repo3 = WatchSettingsRepository(context)
        assertTrue(repo3.isAutoLaunchEnabled())
        assertTrue(repo3.autoLaunchEnabled.value)
        assertTrue(repo3.settings.value.autoLaunchOnSessionStart)
    }
}
