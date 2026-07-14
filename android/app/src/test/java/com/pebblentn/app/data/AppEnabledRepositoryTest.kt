package com.pebblentn.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppEnabledRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun enabledByDefault() {
        assertTrue(AppEnabledRepository(context).isEnabled())
    }

    @Test
    fun setEnabledIsObservableImmediately() {
        val repo = AppEnabledRepository(context)

        repo.setEnabled(false)

        assertFalse(repo.isEnabled())
        assertFalse(repo.enabled.value)
    }

    @Test
    fun settingSurvivesRestart() {
        AppEnabledRepository(context).setEnabled(false)

        // A fresh instance reads the persisted value, as after a process restart.
        assertFalse(AppEnabledRepository(context).isEnabled())
    }
}
