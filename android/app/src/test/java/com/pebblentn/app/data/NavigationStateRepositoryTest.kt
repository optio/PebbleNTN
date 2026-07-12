package com.pebblentn.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pebblentn.app.core.Maneuver
import com.pebblentn.app.core.NavigationInstruction
import com.pebblentn.app.core.NavigationState
import com.pebblentn.app.core.ReducerState
import com.pebblentn.app.data.db.PebbleNtnDatabase
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
class NavigationStateRepositoryTest {

    private lateinit var db: PebbleNtnDatabase
    private lateinit var repo: NavigationStateRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PebbleNtnDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = NavigationStateRepository(db.navigationStateDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun loadWithNoSavedStateIsNull() = runTest {
        assertNull(repo.loadReducerState())
    }

    @Test
    fun savesAndRestoresNavigatingState() = runTest {
        val state = ReducerState(
            current = NavigationState.Navigating(
                sessionId = 5,
                instruction = NavigationInstruction(Maneuver.RIGHT, distanceMeters = 300, primaryText = "Main St"),
                stateTimestampSeconds = 1000,
            ),
            nextSessionId = 6,
            launchedSessionId = 5,
            watchReady = true, // must NOT be restored
        )
        repo.save(state)

        val restored = repo.loadReducerState()!!
        val current = restored.current as NavigationState.Navigating
        assertEquals(5, current.sessionId)
        assertEquals(Maneuver.RIGHT, current.instruction.maneuver)
        assertEquals(300, current.instruction.distanceMeters)
        assertEquals(6, restored.nextSessionId)
        assertEquals(5, restored.launchedSessionId)
        assertFalse("watch readiness must reset on restore", restored.watchReady)
    }

    @Test
    fun overwritesSingletonRow() = runTest {
        repo.save(ReducerState(current = NavigationState.Navigating(1, NavigationInstruction(Maneuver.LEFT), 10)))
        repo.save(ReducerState(current = NavigationState.NoActiveNavigation, nextSessionId = 9))
        val restored = repo.loadReducerState()!!
        assertTrue(restored.current is NavigationState.NoActiveNavigation)
        assertEquals(9, restored.nextSessionId)
    }
}
