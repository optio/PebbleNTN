package com.pebblentn.app.notification.icon

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.R
import com.pebblentn.app.catalog.RuleFamilies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The drawable lookup retries a name under the family's root package, in the posting app's own
 * context (#113 follow-up). The posting app is this test app (its context is the only one Robolectric
 * opens); the fake identifier lookup decides which package names hold the drawable.
 */
@RunWith(RobolectricTestRunner::class)
class AppIconReferencesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val posting = context.packageName
    private val candidates = mapOf("maneuver_turn_normal_left" to "LEFT")

    /** The posting package uses the rules of [ROOT], like MicroG Maps those of Google Maps. */
    private val families = RuleFamilies.of(listOf(setOf(ROOT, posting)), roots = mapOf(posting to ROOT))

    /** A fake lookup with the drawable only under [holders]; records every package name it was asked about. */
    private class FakeIdentifiers(private val holders: Set<String>) : AppIconReferences.DrawableIdentifiers {
        val asked = mutableListOf<String>()
        override fun identifier(resources: android.content.res.Resources, name: String, resourcePackage: String): Int {
            asked += resourcePackage
            return if (resourcePackage in holders) R.drawable.ic_maneuver_left else 0
        }
    }

    @Test
    fun aDrawableFoundOnlyUnderTheRootNameGivesItsManeuver() {
        val lookup = FakeIdentifiers(holders = setOf(ROOT))
        val references = AppIconReferences(context, families, lookup).references(posting, candidates)
        assertEquals(listOf("maneuver_turn_normal_left" to "LEFT"), references.map { it.drawable to it.maneuver })
        assertEquals(listOf(posting, ROOT), lookup.asked)
    }

    @Test
    fun aDrawableFoundUnderThePostingNameNeverTriesTheRoot() {
        val lookup = FakeIdentifiers(holders = setOf(posting, ROOT))
        val references = AppIconReferences(context, families, lookup).references(posting, candidates)
        assertEquals(listOf("LEFT"), references.map { it.maneuver })
        assertEquals(listOf(posting), lookup.asked)
    }

    @Test
    fun noDrawableUnderEitherNameGivesNoReferences() {
        val lookup = FakeIdentifiers(holders = emptySet())
        val references = AppIconReferences(context, families, lookup).references(posting, candidates)
        assertTrue(references.isEmpty())
        assertEquals(listOf(posting, ROOT), lookup.asked)
    }

    @Test
    fun withoutFamiliesOnlyThePostingNameIsTried() {
        val lookup = FakeIdentifiers(holders = setOf(ROOT))
        val references = AppIconReferences(context, RuleFamilies.NONE, lookup).references(posting, candidates)
        assertTrue(references.isEmpty())
        assertEquals(listOf(posting), lookup.asked)
    }

    @Test
    fun theResultIsCachedPerPostingPackageEmptyOnesIncluded() {
        val lookup = FakeIdentifiers(holders = emptySet())
        val icons = AppIconReferences(context, families, lookup)
        icons.references(posting, candidates)
        icons.references(posting, candidates)
        assertEquals("rendered once", listOf(posting, ROOT), lookup.asked)
    }

    private companion object {
        const val ROOT = "com.google.android.apps.maps"
    }
}
