package org.tvheadend.data.source

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.Recording
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [RecordingDataSource] against a real in-memory database.
 *
 * Unlike the other data sources the injected scope is named `scope`; it is passed positionally here
 * so the name does not matter. [Dispatchers.Unconfined] makes every write run inline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecordingDataSourceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppRoomDatabase
    private lateinit var dataSource: RecordingDataSource

    @Before
    fun setUp() {
        TestDatabase.plantNoopTimber()
        db = TestDatabase.create()
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        dataSource = RecordingDataSource(db, CoroutineScope(Dispatchers.Unconfined))
    }

    @After
    fun tearDown() {
        TestDatabase.teardown(db)
    }

    private fun recording(
            id: Int,
            channelId: Int = 0,
            start: Long = 0L,
            stop: Long = 1_000L,
            title: String? = "Recording $id",
            state: String? = "completed",
            error: String? = null,
            contentType: Int = 0,
            duration: Int = 0,
            duplicate: Int = 0,
            eventId: Int = 0
    ): Recording {
        return Recording(
                id = id,
                channelId = channelId,
                start = start,
                stop = stop,
                title = title,
                state = state,
                error = error,
                contentType = contentType,
                duration = duration,
                duplicate = duplicate,
                eventId = eventId,
                connectionId = 1)
    }

    @Test
    fun addItemStoresTheRecording() {
        dataSource.addItem(recording(id = 1))

        assertThat(db.recordingDao.loadRecordingByIdSync(1)!!.id).isEqualTo(1)
    }

    @Test
    fun addItemsStoresAllRecordings() {
        dataSource.addItems(listOf(recording(id = 1), recording(id = 2)))

        assertThat(db.recordingDao.itemCountSync).isEqualTo(2)
    }

    @Test
    fun updateItemReplacesTheStoredRecording() {
        dataSource.addItem(recording(id = 1, title = "Before"))

        dataSource.updateItem(recording(id = 1, title = "After"))

        assertThat(db.recordingDao.loadRecordingByIdSync(1)!!.title).isEqualTo("After")
    }

    @Test
    fun removeItemDeletesTheRecording() {
        dataSource.addItem(recording(id = 1))

        dataSource.removeItem(recording(id = 1))

        assertThat(db.recordingDao.itemCountSync).isEqualTo(0)
    }

    @Test
    fun getLiveDataItemsReturnsTheMappedRecordingsOrderedByStartDescending() {
        dataSource.addItem(recording(id = 1, start = 100L))
        dataSource.addItem(recording(id = 2, start = 200L))

        val items = dataSource.getLiveDataItems().observedValue()!!

        assertThat(items.map { it.id }).isEqualTo(listOf(2, 1))
    }

    @Test
    fun getLiveDataItemByIdReturnsTheRecording() {
        dataSource.addItem(recording(id = 1, title = "One"))

        assertThat(dataSource.getLiveDataItemById(1).observedValue()!!.title).isEqualTo("One")
    }

    @Test
    fun getLiveDataItemsByChannelIdFiltersByChannel() {
        dataSource.addItem(recording(id = 1, channelId = 100))
        dataSource.addItem(recording(id = 2, channelId = 200))

        val items = dataSource.getLiveDataItemsByChannelId(100).observedValue()!!

        assertThat(items.map { it.id }).containsExactly(1)
    }

    @Test
    fun getCompletedRecordingsAppliesTheSortOrder() {
        dataSource.addItem(recording(id = 1, start = 100L))
        dataSource.addItem(recording(id = 2, start = 300L))
        dataSource.addItem(recording(id = 3, start = 200L))

        assertThat(dataSource.getCompletedRecordings(0).observedValue()!!.map { it.id }).isEqualTo(listOf(1, 3, 2))
        assertThat(dataSource.getCompletedRecordings(1).observedValue()!!.map { it.id }).isEqualTo(listOf(2, 3, 1))
    }

    @Test
    fun getScheduledRecordingsHonoursTheDuplicateFlag() {
        dataSource.addItem(recording(id = 1, state = "scheduled", duplicate = 0))
        dataSource.addItem(recording(id = 2, state = "scheduled", duplicate = 1))

        val withDuplicates = dataSource.getScheduledRecordings(hideDuplicates = false).observedValue()!!.map { it.id }
        val withoutDuplicates = dataSource.getScheduledRecordings(hideDuplicates = true).observedValue()!!.map { it.id }

        assertThat(withDuplicates).containsExactly(1, 2)
        assertThat(withoutDuplicates).containsExactly(1)
    }

    @Test
    fun getFailedRecordingsReturnsMissedRecordings() {
        dataSource.addItem(recording(id = 1, state = "missed"))
        dataSource.addItem(recording(id = 2, state = "completed"))

        assertThat(dataSource.getFailedRecordings().observedValue()!!.map { it.id }).containsExactly(1)
    }

    @Test
    fun getRemovedRecordingsReturnsFileMissingRecordings() {
        dataSource.addItem(recording(id = 1, state = "completed", error = "File missing"))
        dataSource.addItem(recording(id = 2, state = "completed"))

        assertThat(dataSource.getRemovedRecordings().observedValue()!!.map { it.id }).containsExactly(1)
    }

    @Test
    fun getLiveDataCountByTypeMapsEveryKnownType() {
        dataSource.addItem(recording(id = 1, state = "completed"))
        dataSource.addItem(recording(id = 2, state = "recording"))
        dataSource.addItem(recording(id = 3, state = "scheduled"))
        dataSource.addItem(recording(id = 4, state = "missed"))
        dataSource.addItem(recording(id = 5, state = "completed", error = "File missing"))

        assertThat(dataSource.getLiveDataCountByType("completed").observedValue()).isEqualTo(1)
        assertThat(dataSource.getLiveDataCountByType("scheduled").observedValue()).isEqualTo(2)
        assertThat(dataSource.getLiveDataCountByType("running").observedValue()).isEqualTo(1)
        assertThat(dataSource.getLiveDataCountByType("failed").observedValue()).isEqualTo(1)
        assertThat(dataSource.getLiveDataCountByType("removed").observedValue()).isEqualTo(1)
    }

    @Test
    fun getLiveDataCountByTypeReturnsAnEmptyLiveDataForAnUnknownType() {
        assertThat(dataSource.getLiveDataCountByType("unknown").observedValue()).isNull()
    }

    @Test
    fun getItemByIdReturnsTheRecordingOnlyForPositiveIds() {
        dataSource.addItem(recording(id = 1, title = "One"))

        assertThat(dataSource.getItemById(1)!!.title).isEqualTo("One")
        assertThat(dataSource.getItemById(0)).isNull()
        assertThat(dataSource.getItemById(-1)).isNull()
    }

    @Test
    fun getItemByEventIdReturnsTheRecordingOnlyForPositiveIds() {
        dataSource.addItem(recording(id = 1, eventId = 55))

        assertThat(dataSource.getItemByEventId(55)!!.id).isEqualTo(1)
        assertThat(dataSource.getItemByEventId(0)).isNull()
    }

    @Test
    fun getItemsReturnsAnEmptyList() {
        dataSource.addItem(recording(id = 1))

        // The list variant is intentionally not implemented and always returns an empty list.
        assertThat(dataSource.getItems()).isEmpty()
    }

    @Test
    fun getLiveDataItemCountReturnsAnEmptyLiveData() {
        dataSource.addItem(recording(id = 1))

        assertThat(dataSource.getLiveDataItemCount().observedValue()).isNull()
    }

    @Test
    fun removeAndAddItemsReplacesEverything() {
        dataSource.addItem(recording(id = 1))
        dataSource.addItem(recording(id = 2))

        dataSource.removeAndAddItems(arrayListOf(recording(id = 3), recording(id = 4)))

        assertThat(db.recordingDao.itemCountSync).isEqualTo(2)
        assertThat(db.recordingDao.loadRecordingByIdSync(1)).isNull()
        assertThat(db.recordingDao.loadRecordingByIdSync(3)).isNotNull()
    }
}
