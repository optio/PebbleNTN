package com.pebblentn.app.catalog

import com.pebblentn.app.catalog.PackageCheck.Reason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** FR5: a user-added package name is well formed and used by no other package of PebbleNTN. */
class CustomPackageValidatorTest {

    private val builtIn = mapOf(
        "com.google.android.apps.maps" to "google-maps",
        "app.morphe.android.apps.maps" to "microg-maps",
        "net.osmand" to "osmand",
        "com.baidu.BaiduMap" to "baidu-maps",
    )

    private fun check(input: String, appId: String = "microg-maps", userAdded: Map<String, List<String>> = emptyMap()) =
        CustomPackageValidator.check(input, appId, builtIn, userAdded)

    @Test
    fun aNewWellFormedNameIsAcceptedTrimmedWithItsCaseKept() {
        // Android package names are case-sensitive: stored exactly as typed.
        assertEquals(PackageCheck.Valid("App.ReVanced.android.apps.maps"), check("  App.ReVanced.android.apps.maps \n"))
        assertEquals(PackageCheck.Valid("com.example_2.maps9"), check("com.example_2.maps9"))
    }

    @Test
    fun namesDifferingOnlyInCaseAreDuplicates() {
        val added = mapOf("microg-maps" to listOf("App.Patched.maps"), "osmand" to listOf("net.OsmAnd.Beta"))
        assertEquals(PackageCheck.Invalid(Reason.ADDED_HERE), check("app.patched.MAPS", userAdded = added))
        assertEquals(PackageCheck.Invalid(Reason.ADDED_ELSEWHERE, "osmand"), check("NET.osmand.beta", userAdded = added))
        assertEquals(PackageCheck.Invalid(Reason.BUILT_IN_ELSEWHERE, "google-maps"), check("Com.Google.Android.Apps.Maps"))
        assertEquals(PackageCheck.Invalid(Reason.BUILT_IN_ELSEWHERE, "baidu-maps"), check("com.baidu.BaiduMap"))
    }

    @Test
    fun malformedNamesAreRejected() {
        for (input in listOf("maps", "com.", ".com.maps", "com..maps", "com.1maps", "com.maps-patched", "com maps.x", "com/maps.x", "é.maps")) {
            assertEquals(input, PackageCheck.Invalid(Reason.FORMAT), check(input))
        }
        assertEquals(PackageCheck.Invalid(Reason.EMPTY), check("   "))
        assertEquals(PackageCheck.Invalid(Reason.TOO_LONG), check("a." + "b".repeat(CustomPackageValidator.MAX_LENGTH)))
    }

    @Test
    fun aBuiltInPackageOfAnotherAppIsRejectedNamingThatApp() {
        assertEquals(PackageCheck.Invalid(Reason.BUILT_IN_ELSEWHERE, "google-maps"), check("com.google.android.apps.maps"))
        // Case does not hide a duplicate of a bundled name with capitals.
        assertEquals(PackageCheck.Invalid(Reason.BUILT_IN_ELSEWHERE, "baidu-maps"), check("com.baidu.baidumap"))
    }

    @Test
    fun aBuiltInPackageOfTheSameAppIsRejected() {
        assertEquals(PackageCheck.Invalid(Reason.BUILT_IN_HERE), check(" APP.morphe.android.apps.maps"))
    }

    @Test
    fun aPackageAddedToAnotherAppIsRejectedNamingThatApp() {
        val added = mapOf("osmand" to listOf("net.osmand.patched"))
        assertEquals(PackageCheck.Invalid(Reason.ADDED_ELSEWHERE, "osmand"), check("net.osmand.patched", userAdded = added))
    }

    @Test
    fun aPackageAlreadyAddedToTheSameAppIsRejected() {
        val added = mapOf("microg-maps" to listOf("app.revanced.android.apps.maps"))
        assertEquals(PackageCheck.Invalid(Reason.ADDED_HERE), check("app.revanced.android.apps.maps ", userAdded = added))
    }

    @Test
    fun wellFormedMeansTheStoredForm() {
        assertTrue(CustomPackageValidator.isWellFormed("app.revanced.android.apps.maps"))
        assertTrue(CustomPackageValidator.isWellFormed("com.baidu.BaiduMap"))
        assertFalse(CustomPackageValidator.isWellFormed(" app.maps"))
        assertFalse(CustomPackageValidator.isWellFormed("app.1maps"))
    }
}
