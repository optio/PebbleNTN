package com.pebblentn.app.ui

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.data.DebugHistoryRepository
import com.pebblentn.app.data.db.PebbleNtnDatabase
import com.pebblentn.app.notification.NotificationSnapshot
import com.pebblentn.app.notification.PostedNotification
import com.pebblentn.app.ui.debug.DebugHistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DebugHistoryViewModelTest {

    private lateinit var db: PebbleNtnDatabase
    private lateinit var repo: DebugHistoryRepository
    private lateinit var vm: DebugHistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PebbleNtnDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = DebugHistoryRepository(db.debugEventDao())
        vm = DebugHistoryViewModel(repo)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun record(at: Long) = PostedNotification(
        snapshot = NotificationSnapshot(packageName = "com.waze", notificationId = 1, postTimeMillis = at, title = "T$at"),
        notificationKey = "k$at",
        tag = null,
        receivedAtMillis = at,
    )

    @Test
    fun detailReturnsStoredEvent() = runTest {
        val id = repo.recordPosted(record(10))
        val detail = vm.detail(id)
        assertEquals("T10", detail?.snapshot?.title)
    }

    @Test
    fun deleteEventRemovesIt() = runTest {
        val id = repo.recordPosted(record(10))
        vm.deleteEvent(id).join()
        assertNull(vm.detail(id))
        assertEquals(0, repo.count())
    }

    @Test
    fun deleteAllClearsHistory() = runTest {
        repo.recordPosted(record(1))
        repo.recordPosted(record(2))
        vm.deleteAll().join()
        assertEquals(0, repo.count())
    }
}
