package com.pebblentn.app.ui.dashboard

import com.pebblentn.app.update.InstallSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** REQ-ANDROID-020 (FR5.2, FR5.4): the update card's main source, "Other sources" and the delay line. */
class UpdateCardTest {

    @Test
    fun fdroidCardShowsTheDelayAndOffersGitHubAndObtainium() {
        val model = updateCardModel(InstallSource.FDROID)
        assertEquals(InstallSource.FDROID, model.main)
        assertEquals(listOf(InstallSource.GITHUB, InstallSource.OBTAINIUM), model.others)
        assertTrue(model.showFdroidDelay)
    }

    @Test
    fun gitHubCardHasNoDelayAndOffersFdroidAndObtainium() {
        val model = updateCardModel(InstallSource.GITHUB)
        assertEquals(InstallSource.GITHUB, model.main)
        assertEquals(listOf(InstallSource.FDROID, InstallSource.OBTAINIUM), model.others)
        assertFalse(model.showFdroidDelay)
    }

    @Test
    fun obtainiumCardHasNoDelayAndOffersGitHubAndFdroid() {
        val model = updateCardModel(InstallSource.OBTAINIUM)
        assertEquals(InstallSource.OBTAINIUM, model.main)
        assertEquals(listOf(InstallSource.GITHUB, InstallSource.FDROID), model.others)
        assertFalse(model.showFdroidDelay)
    }
}
