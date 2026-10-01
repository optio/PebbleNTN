package com.pebblentn.app.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** #28 / spec/400-ui: the Pebble monochrome theme is optional and off by default. */
@RunWith(RobolectricTestRunner::class)
class AppearanceRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun offByDefault() {
        assertFalse(AppearanceRepository(context).monochrome.value)
    }

    @Test
    fun theChoiceIsRemembered() {
        AppearanceRepository(context).setMonochrome(true)
        assertTrue(AppearanceRepository(context).monochrome.value)
    }
}
