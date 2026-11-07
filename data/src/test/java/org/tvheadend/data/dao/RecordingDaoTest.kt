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
import org.tvheadend.data.entity.Recording
import org.tvheadend.data.entity.RecordingEntity
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [RecordingDao] against a real in-memory database.
 *
 * The `recordings` table has a composite primary key of `(id, connection_id)` and every count and
 * list query filters on the active connection, so most tests plant both an active and an inactive
 * connection.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecordingDaoTest {

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
    fun countsRecordingsPerStateOnlyForTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.recordingDao.insert(TestDatabase.recording(id = 1, connectionId = 1, state = "completed"))
        db.recordingDao.insert(TestDatabase.recording(id = 2, connectionId = 1, state = "recording"))
        db.recordingDao.insert(TestDatabase.recording(id = 3, connectionId = 1, state = "scheduled"))
        db.recordingDao.insert(TestDatabase.recording(id = 4, connectionId = 1, state = "missed"))
        db.recordingDao.insert(TestDatabase.recording(id = 5, connectionId = 1, state = "completed", error = "Aborted by user"))
        db.recordingDao.insert(TestDatabase.recording(id = 6, connectionId = 1, state = "completed", error = "File missing"))
        // Belongs to the inactive connection and must never be counted.
        db.recordingDao.insert(TestDatabase.recording(id = 7, connectionId = 2, state = "completed"))

        assertThat(db.recordingDao.itemCountSync).isEqualTo(6)
        assertThat(db.recordingDao.completedRecordingCount.observedValue()).isEqualTo(1)
        assertThat(db.recordingDao.scheduledRecordingCount.observedValue()).isEqualTo(2)
        assertThat(db.recordingDao.runningRecordingCount.observedValue()).isEqualTo(1)
        assertThat(db.recordingDao.failedRecordingCount.observedValue()).isEqualTo(2)
        assertThat(db.recordingDao.removedRecordingCount.observedValue()).isEqualTo(1)
    }

    @Test
    fun loadRecordingsReturnsTheActiveConnectionRecordingsOrderedByStartDescending() {
        insertActiveAndInactiveConnections()
        db.recordingDao.insert(TestDatabase.recording(id = 1, connectionId = 1, start = 100L))
        db.recordingDao.insert(TestDatabase.recording(id = 2, connectionId = 1, start = 300L))
        db.recordingDao.insert(TestDatabase.recording(id = 3, connectionId = 2, start = 500L))

        val ids = db.recordingDao.loadRecordings().observedValue()!!.map { it.id }

        assertThat(ids).isEqualTo(listOf(2, 1))
    }

    @Test
    fun loadCompletedRecordingsAppliesTheRequestedSortOrder() {
        insertActiveAndInactiveConnections()
        // start, title, content_type and duration deliberately disagree so each sort is verifiable.
        db.recordingDao.insert(TestDatabase.recording(id = 1, connectionId = 1, start = 100L, title = "Bravo", contentType = 2, duration = 30))
        db.recordingDao.insert(TestDatabase.recording(id = 2, connectionId = 1, start = 300L, title = "Alpha", contentType = 1, duration = 10))
        db.recordingDao.insert(TestDatabase.recording(id = 3, connectionId = 1, start = 200L, title = "Charlie", contentType = 3, duration = 20))

        assertThat(idsForCompletedSortOrder(0)).isEqualTo(listOf(1, 3, 2))
        assertThat(idsForCompletedSortOrder(1)).isEqualTo(listOf(2, 3, 1))
        assertThat(idsForCompletedSortOrder(2)).isEqualTo(listOf(2, 1, 3))
        assertThat(idsForCompletedSortOrder(3)).isEqualTo(listOf(3, 1, 2))
        assertThat(idsForCompletedSortOrder(4)).isEqualTo(listOf(2, 1, 3))
        assertThat(idsForCompletedSortOrder(5)).isEqualTo(listOf(3, 1, 2))
        assertThat(idsForCompletedSortOrder(6)).isEqualTo(listOf(2, 3, 1))
        assertThat(idsForCompletedSortOrder(7)).isEqualTo(listOf(1, 3, 2))
    }

    private fun idsForCompletedSortOrder(sortOrder: Int): List<Int> {
        return db.recordingDao.loadCompletedRecordings(sortOrder).observedValue()!!.map { it.id }
    }

    @Test
    fun loadScheduledRecordingsDistinguishesDuplicates() {
        insertActiveAndInactiveConnections()
        db.recordingDao.insert(TestDatabase.recording(id = 1, connectionId = 1, state = "recording", duplicate = 0))
        db.recordingDao.insert(TestDatabase.recording(id = 2, connectionId = 1, state = "scheduled", duplicate = 1))
        db.recordingDao.insert(TestDatabase.recording(id = 3, connectionId = 1, state = "scheduled", duplicate = 0))
        db.recordingDao.insert(TestDatabase.recording(id = 4, connectionId = 2, state = "scheduled", duplicate = 0))

        val unique = db.recordingDao.loadUniqueScheduledRecordings().observedValue()!!.map { it.id }
        val all = db.recordingDao.loadScheduledRecordings().observedValue()!!.map { it.id }

        assertThat(unique).containsExactly(1, 3)
        assertThat(all).containsExactly(1, 2, 3)
    }

    /**
     * The list query and the count query must agree on which recordings are failed: only the ones
     * that belong to the active connection. The OR-branches of `loadFailedRecordings` are now
     * wrapped in parentheses, so `connection_id IN (active)` guards every branch and the inactive
     * connection's recording no longer leaks into the list.
     */
    @Test
    fun loadFailedRecordingsOnlyReturnsRecordingsOfTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.recordingDao.insert(TestDatabase.recording(id = 1, connectionId = 1, state = "missed"))
        // This recording belongs to the inactive connection and must not be returned.
        db.recordingDao.insert(TestDatabase.recording(id = 2, connectionId = 2, state = "missed"))

        val failedIds = db.recordingDao.loadFailedRecordings().observedValue()!!.map { it.id }
        assertThat(failedIds).containsExactly(1)
        // The count query and the list query must report the same set of recordings.
        assertThat(db.recordingDao.failedRecordingCount.observedValue()).isEqualTo(failedIds.size)
        assertThat(db.recordingDao.failedRecordingCount.observedValue()).isEqualTo(1)
    }

    @Test
    fun loadRemovedRecordingsReturnsFileMissingCompletedRecordings() {
        insertActiveAndInactiveConnections()
        db.recordingDao.insert(TestDatabase.recording(id = 1, connectionId = 1, state = "completed", error = "File missing"))
        db.recordingDao.insert(TestDatabase.recording(id = 2, connectionId = 1, state = "completed"))

        val ids = db.recordingDao.loadRemovedRecordings().observedValue()!!.map { it.id }

        assertThat(ids).containsExactly(1)
    }

    @Test
    fun loadRecordingByIdOnlyReturnsRecordingsOfTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.recordingDao.insert(TestDatabase.recording(id = 1, connectionId = 1, title = "Active"))
        db.recordingDao.insert(TestDatabase.recording(id = 1, connectionId = 2, title = "Inactive"))

        assertThat(db.recordingDao.loadRecordingById(1).observedValue()!!.title).isEqualTo("Active")
        assertThat(db.recordingDao.loadRecordingByIdSync(1)!!.title).isEqualTo("Active")
        assertThat(db.recordingDao.loadRecordingByIdSync(999)).isNull()
    }

    @Test
    fun loadRecordingsByChannelIdFiltersByChannel() {
        insertActiveAndInactiveConnections()
        db.recordingDao.insert(TestDatabase.recording(id = 1, connectionId = 1, channelId = 100))
        db.recordingDao.insert(TestDatabase.recording(id = 2, connectionId = 1, channelId = 200))

        val ids = db.recordingDao.loadRecordingsByChannelId(100).observedValue()!!.map { it.id }

        assertThat(ids).containsExactly(1)
    }

    @Test
    fun loadRecordingByEventIdSyncFiltersByEventId() {
        insertActiveAndInactiveConnections()
        db.recordingDao.insert(TestDatabase.recording(id = 1, connectionId = 1, eventId = 55))
        db.recordingDao.insert(TestDatabase.recording(id = 2, connectionId = 1, eventId = 66))

        assertThat(db.recordingDao.loadRecordingByEventIdSync(55)!!.id).isEqualTo(1)
        assertThat(db.recordingDao.loadRecordingByEventIdSync(999)).isNull()
    }

    @Test
    fun insertsUpdatesAndDeletesRecordings() {
        insertActiveAndInactiveConnections()
        db.recordingDao.insert(TestDatabase.recording(id = 1, connectionId = 1, title = "One"))
        db.recordingDao.insert(listOf(
                TestDatabase.recording(id = 2, connectionId = 1, title = "Two"),
                TestDatabase.recording(id = 3, connectionId = 1, title = "Three")))
        assertThat(db.recordingDao.itemCountSync).isEqualTo(3)

        db.recordingDao.update(TestDatabase.recording(id = 1, connectionId = 1, title = "Uno"))
        assertThat(db.recordingDao.loadRecordingByIdSync(1)!!.title).isEqualTo("Uno")

        db.recordingDao.delete(TestDatabase.recording(id = 1, connectionId = 1))
        assertThat(db.recordingDao.loadRecordingByIdSync(1)).isNull()

        db.recordingDao.delete(listOf(TestDatabase.recording(id = 2, connectionId = 1)))
        assertThat(db.recordingDao.loadRecordingByIdSync(2)).isNull()

        db.recordingDao.deleteById(3)
        assertThat(db.recordingDao.itemCountSync).isEqualTo(0)

        db.recordingDao.insert(TestDatabase.recording(id = 4, connectionId = 1))
        db.recordingDao.deleteAll()
        assertThat(db.recordingDao.itemCountSync).isEqualTo(0)
    }

    @Test
    fun mapsRecordingToEntityAndBack() {
        val recording = Recording(
                id = 4,
                channelId = 2,
                start = 1_000L,
                stop = 2_000L,
                title = "Title",
                state = "completed",
                contentType = 1,
                connectionId = 1)

        val restored = RecordingEntity.from(recording).toRecording()

        assertThat(restored).isEqualTo(recording)
    }
}
