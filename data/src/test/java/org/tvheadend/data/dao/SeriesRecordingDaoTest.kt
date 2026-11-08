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
import org.tvheadend.data.entity.SeriesRecording
import org.tvheadend.data.entity.SeriesRecordingEntity
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [SeriesRecordingDao] against a real in-memory database.
 *
 * The `series_recordings` table has a composite `(id, connection_id)` primary key where the id is
 * a String UUID. Every query filters on the active connection.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SeriesRecordingDaoTest {

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
        db.seriesRecordingDao.insert(TestDatabase.seriesRecording(id = "a", connectionId = 1))
        db.seriesRecordingDao.insert(TestDatabase.seriesRecording(id = "b", connectionId = 1))
        db.seriesRecordingDao.insert(TestDatabase.seriesRecording(id = "c", connectionId = 2))

        assertThat(db.seriesRecordingDao.itemCountSync).isEqualTo(2)
        assertThat(db.seriesRecordingDao.itemCount.observedValue()).isEqualTo(2)
    }

    @Test
    fun loadAllRecordingsOrdersByStartThenTitle() {
        insertActiveAndInactiveConnections()
        db.seriesRecordingDao.insert(TestDatabase.seriesRecording(id = "s1", connectionId = 1, start = 100L, title = "Bravo"))
        db.seriesRecordingDao.insert(TestDatabase.seriesRecording(id = "s2", connectionId = 1, start = 100L, title = "Alpha"))
        db.seriesRecordingDao.insert(TestDatabase.seriesRecording(id = "s3", connectionId = 1, start = 50L, title = "Charlie"))
        db.seriesRecordingDao.insert(TestDatabase.seriesRecording(id = "s4", connectionId = 2, start = 1L, title = "Hidden"))

        val ids = db.seriesRecordingDao.loadAllRecordings().observedValue()!!.map { it.id }

        assertThat(ids).isEqualTo(listOf("s3", "s2", "s1"))
    }

    @Test
    fun loadAllRecordingsResolvesTheChannelNameFromTheJoinedChannel() {
        insertActiveAndInactiveConnections()
        db.channelDao.insert(TestDatabase.channel(id = 5, connectionId = 1, name = "Live Channel"))
        db.seriesRecordingDao.insert(TestDatabase.seriesRecording(id = "a", connectionId = 1, channelId = 5))

        val loaded = db.seriesRecordingDao.loadAllRecordings().observedValue()!!.single()

        // series_recordings has its own channel_name column (null here). The query projected
        // rec.* before the join aliases, so getColumnIndex picked the table's column and the JOIN
        // was dead; the live name from `channels` has to win.
        assertThat(loaded.channelName).isEqualTo("Live Channel")
    }

    @Test
    fun loadRecordingByIdOnlyReturnsRecordingsOfTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.seriesRecordingDao.insert(TestDatabase.seriesRecording(id = "same", connectionId = 1, title = "Active"))
        db.seriesRecordingDao.insert(TestDatabase.seriesRecording(id = "same", connectionId = 2, title = "Inactive"))

        assertThat(db.seriesRecordingDao.loadRecordingById("same").observedValue()!!.title).isEqualTo("Active")
        assertThat(db.seriesRecordingDao.loadRecordingByIdSync("same")!!.title).isEqualTo("Active")
        assertThat(db.seriesRecordingDao.loadRecordingByIdSync("missing")).isNull()
    }

    @Test
    fun insertsUpdatesAndDeletesSeriesRecordings() {
        insertActiveAndInactiveConnections()
        db.seriesRecordingDao.insert(TestDatabase.seriesRecording(id = "a", connectionId = 1, title = "One"))
        assertThat(db.seriesRecordingDao.itemCountSync).isEqualTo(1)

        db.seriesRecordingDao.update(TestDatabase.seriesRecording(id = "a", connectionId = 1, title = "Uno"))
        assertThat(db.seriesRecordingDao.loadRecordingByIdSync("a")!!.title).isEqualTo("Uno")

        db.seriesRecordingDao.delete(TestDatabase.seriesRecording(id = "a", connectionId = 1))
        assertThat(db.seriesRecordingDao.itemCountSync).isEqualTo(0)

        db.seriesRecordingDao.insert(TestDatabase.seriesRecording(id = "b", connectionId = 1))
        db.seriesRecordingDao.deleteById("b")
        assertThat(db.seriesRecordingDao.itemCountSync).isEqualTo(0)

        db.seriesRecordingDao.insert(TestDatabase.seriesRecording(id = "c", connectionId = 1))
        db.seriesRecordingDao.deleteAll()
        assertThat(db.seriesRecordingDao.itemCountSync).isEqualTo(0)
    }

    @Test
    fun mapsSeriesRecordingToEntityAndBack() {
        val recording = SeriesRecording(
                id = "uuid",
                isEnabled = false,
                name = "Name",
                minDuration = 10,
                maxDuration = 100,
                start = 60L,
                startWindow = 120L,
                title = "Title",
                channelId = 5,
                connectionId = 1)

        val restored = SeriesRecordingEntity.from(recording).toRecording()

        assertThat(restored).isEqualTo(recording)
    }
}
