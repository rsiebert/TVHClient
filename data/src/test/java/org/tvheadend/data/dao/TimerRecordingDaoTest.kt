package org.tvheadend.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.TimerRecording
import org.tvheadend.data.entity.TimerRecordingEntity
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [TimerRecordingDao] against a real in-memory database.
 *
 * The `timer_recordings` table has a composite `(id, connection_id)` primary key where the id is
 * a String UUID. Every query filters on the active connection.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimerRecordingDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppRoomDatabase

    @Before
    fun setUp() {
        TestDatabase.plantNoopTimber()
        db = TestDatabase.create()
    }

    @After
    fun tearDown() {
        TestDatabase.teardown(db)
    }

    private fun insertActiveAndInactiveConnections() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        db.connectionDao.insert(TestDatabase.inactiveConnection(id = 2))
    }

    @Test
    fun countsOnlyTheRecordingsOfTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.timerRecordingDao.insert(TestDatabase.timerRecording(id = "a", connectionId = 1))
        db.timerRecordingDao.insert(TestDatabase.timerRecording(id = "b", connectionId = 1))
        db.timerRecordingDao.insert(TestDatabase.timerRecording(id = "c", connectionId = 2))

        assertThat(db.timerRecordingDao.itemCountSync).isEqualTo(2)
        assertThat(db.timerRecordingDao.itemCount.observedValue()).isEqualTo(2)
    }

    @Test
    fun loadAllRecordingsOrdersByStartThenTitle() {
        insertActiveAndInactiveConnections()
        db.timerRecordingDao.insert(TestDatabase.timerRecording(id = "s1", connectionId = 1, start = 100L, title = "Bravo"))
        db.timerRecordingDao.insert(TestDatabase.timerRecording(id = "s2", connectionId = 1, start = 100L, title = "Alpha"))
        db.timerRecordingDao.insert(TestDatabase.timerRecording(id = "s3", connectionId = 1, start = 50L, title = "Charlie"))
        db.timerRecordingDao.insert(TestDatabase.timerRecording(id = "s4", connectionId = 2, start = 1L, title = "Hidden"))

        val ids = db.timerRecordingDao.loadAllRecordings().observedValue()!!.map { it.id }

        assertThat(ids).isEqualTo(listOf("s3", "s2", "s1"))
    }

    @Test
    fun loadRecordingByIdOnlyReturnsRecordingsOfTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.timerRecordingDao.insert(TestDatabase.timerRecording(id = "same", connectionId = 1, title = "Active"))
        db.timerRecordingDao.insert(TestDatabase.timerRecording(id = "same", connectionId = 2, title = "Inactive"))

        assertThat(db.timerRecordingDao.loadRecordingById("same").observedValue()!!.title).isEqualTo("Active")
        assertThat(db.timerRecordingDao.loadRecordingByIdSync("same")!!.title).isEqualTo("Active")
        assertThat(db.timerRecordingDao.loadRecordingByIdSync("missing")).isNull()
    }

    @Test
    fun insertsUpdatesAndDeletesTimerRecordings() {
        insertActiveAndInactiveConnections()
        db.timerRecordingDao.insert(TestDatabase.timerRecording(id = "a", connectionId = 1, title = "One"))
        assertThat(db.timerRecordingDao.itemCountSync).isEqualTo(1)

        db.timerRecordingDao.update(TestDatabase.timerRecording(id = "a", connectionId = 1, title = "Uno"))
        assertThat(db.timerRecordingDao.loadRecordingByIdSync("a")!!.title).isEqualTo("Uno")

        db.timerRecordingDao.delete(TestDatabase.timerRecording(id = "a", connectionId = 1))
        assertThat(db.timerRecordingDao.itemCountSync).isEqualTo(0)

        db.timerRecordingDao.insert(TestDatabase.timerRecording(id = "b", connectionId = 1))
        db.timerRecordingDao.deleteById("b")
        assertThat(db.timerRecordingDao.itemCountSync).isEqualTo(0)

        db.timerRecordingDao.insert(TestDatabase.timerRecording(id = "c", connectionId = 1))
        db.timerRecordingDao.deleteAll()
        assertThat(db.timerRecordingDao.itemCountSync).isEqualTo(0)
    }

    @Test
    fun mapsTimerRecordingToEntityAndBack() {
        val recording = TimerRecording(
                id = "uuid",
                title = "Title",
                isEnabled = false,
                name = "Name",
                configName = "config",
                channelId = 5,
                start = 60L,
                stop = 120L,
                connectionId = 1)

        val restored = TimerRecordingEntity.from(recording).toRecording()

        assertThat(restored).isEqualTo(recording)
    }
}
