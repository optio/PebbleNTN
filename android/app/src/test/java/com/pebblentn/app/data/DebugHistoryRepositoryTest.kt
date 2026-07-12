package com.pebblentn.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.core.Hashing
import com.pebblentn.app.data.db.PebbleNtnDatabase
import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.notification.PostedNotification
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DebugHistoryRepositoryTest {

    private lateinit var db: PebbleNtnDatabase
    private lateinit var repo: DebugHistoryRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PebbleNtnDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = DebugHistoryRepository(db.debugEventDao())
    }

    @After
    fun tearDown() = db.close()

    private fun event(pkg: String, at: Long, title: String = "Turn right", key: String = "k$at", tag: String? = null) =
        PostedNotification(
            snapshot = NotificationSnapshot(packageName = pkg, notificationId = 1, postTimeMillis = at, title = title),
            notificationKey = key,
            tag = tag,
            receivedAtMillis = at,
        )

    @Test
    fun storesAndReadsBackSnapshot() = runTest {
        repo.recordPosted(event("com.google.android.apps.maps", at = 100, title = "Turn left"))
        val events = repo.observeRecent().first()
        assertEquals(1, events.size)
        assertEquals("Turn left", events.single().snapshot?.title)
        assertEquals("com.google.android.apps.maps", events.single().packageName)
    }

    @Test
    fun keyAndTagAreStoredHashedNotRaw() = runTest {
        val id = repo.recordPosted(event("com.waze", at = 1, key = "raw-key-123", tag = "raw-tag-abc"))
        val entity = db.debugEventDao().getById(id)!!
        assertEquals(Hashing.sha256Hex("raw-key-123"), entity.notificationKeyHash)
        assertEquals(Hashing.sha256Hex("raw-tag-abc"), entity.tagHash)
        assertFalse(entity.notificationKeyHash.contains("raw-key"))
    }

    @Test
    fun nullTagStoresNullHash() = runTest {
        val id = repo.recordPosted(event("com.waze", at = 1, tag = null))
        assertNull(db.debugEventDao().getById(id)!!.tagHash)
    }

    @Test
    fun retentionTrimsOldestBeyondLimit() = runTest {
        repo.retentionLimit = 3
        repeat(6) { i -> repo.recordPosted(event("com.waze", at = i.toLong())) }

        assertEquals(3, repo.count())
        // Newest three (received at 3,4,5) survive; oldest are trimmed.
        val survivingTimes = repo.observeRecent().first().map { it.receivedTimestampMillis }.toSet()
        assertEquals(setOf(3L, 4L, 5L), survivingTimes)
    }

    @Test
    fun unlimitedRetentionKeepsEverything() = runTest {
        repo.retentionLimit = DebugHistoryRepository.UNLIMITED
        repeat(10) { i -> repo.recordPosted(event("com.waze", at = i.toLong())) }
        assertEquals(10, repo.count())
    }

    @Test
    fun deleteByIdAndDeleteAll() = runTest {
        val id = repo.recordPosted(event("com.waze", at = 1))
        repo.recordPosted(event("com.waze", at = 2))
        repo.deleteById(id)
        assertEquals(1, repo.count())
        repo.deleteAll()
        assertEquals(0, repo.count())
    }

    @Test
    fun recentAreOrderedNewestFirst() = runTest {
        repo.recordPosted(event("com.waze", at = 10))
        repo.recordPosted(event("com.waze", at = 30))
        repo.recordPosted(event("com.waze", at = 20))
        val times = repo.observeRecent().first().map { it.receivedTimestampMillis }
        assertEquals(listOf(30L, 20L, 10L), times)
    }
}
