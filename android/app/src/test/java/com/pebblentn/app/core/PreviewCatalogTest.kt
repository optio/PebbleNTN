package com.pebblentn.app.core

import com.pebblentn.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every visual watch setting value has its preview on every image set, and no `watch_preview_*`
 * image is left without a value (REQ-ANDROID-021, #102 FR6/FR7). A missing image fails the build
 * already (PreviewCatalog names each one); this also catches an image mapped to the wrong value and
 * one the capture tool wrote that nothing uses.
 */
class PreviewCatalogTest {

    private val drawableFields: Map<String, Int> = R.drawable::class.java.fields
        .filter { it.name.startsWith("watch_preview_") }
        .associate { it.name to it.getInt(null) }

    private fun platformOf(set: PreviewCatalog.ImageSet) = when (set) {
        PreviewCatalog.ImageSet.BASALT -> WatchPlatform.BASALT
        PreviewCatalog.ImageSet.CHALK -> WatchPlatform.CHALK
        PreviewCatalog.ImageSet.EMERY -> WatchPlatform.EMERY
    }

    @Test
    fun everyVisualValueHasItsOwnImageOnEverySet() {
        for (set in PreviewCatalog.ImageSet.entries) {
            for (setting in PreviewCatalog.VISUAL_SETTINGS) {
                val names = PreviewCatalog.VALUE_NAMES.getValue(setting)
                assertEquals("$setting names one image per value", setting.valueCount, names.size)
                for (value in 0 until setting.valueCount) {
                    val drawable = PreviewCatalog.drawable(setting, value, platformOf(set))
                    assertNotNull("$setting $value on $set", drawable)
                    val name = PreviewCatalog.resourceName(setting, value, set)!!
                    assertEquals("$name is the image of $setting $value", drawableFields[name], drawable)
                }
            }
        }
    }

    @Test
    fun noImageIsLeftWithoutAValue() {
        val expected = PreviewCatalog.ImageSet.entries.flatMap { set ->
            PreviewCatalog.VISUAL_SETTINGS.flatMap { setting ->
                (0 until setting.valueCount).map { PreviewCatalog.resourceName(setting, it, set)!! }
            }
        }.toSet()
        assertEquals(29 * 3, expected.size)
        assertEquals(expected, drawableFields.keys)
    }

    @Test
    fun blackAndWhiteWatchesUseBasaltInGreyscale() {
        for (platform in listOf(WatchPlatform.APLITE, WatchPlatform.DIORITE)) {
            assertEquals(PreviewCatalog.ImageSet.BASALT, PreviewCatalog.imageSet(platform))
            assertTrue(PreviewCatalog.isGreyscale(platform))
            assertEquals(
                R.drawable.watch_preview_accent_white_basalt,
                PreviewCatalog.drawable(WatchSetting.ACCENT, WatchValues.ACCENT_WHITE, platform),
            )
        }
        assertEquals("basalt before the platform is known", PreviewCatalog.ImageSet.BASALT, PreviewCatalog.imageSet(WatchPlatform.UNKNOWN))
        assertFalse(PreviewCatalog.isGreyscale(WatchPlatform.UNKNOWN))
        assertFalse(PreviewCatalog.isGreyscale(WatchPlatform.EMERY))
    }

    @Test
    fun nonVisualSettingsHaveNoPreview() {
        for (setting in WatchSetting.entries - PreviewCatalog.VISUAL_SETTINGS.toSet()) {
            assertEquals(null, PreviewCatalog.drawable(setting, 0, WatchPlatform.BASALT))
        }
    }
}
