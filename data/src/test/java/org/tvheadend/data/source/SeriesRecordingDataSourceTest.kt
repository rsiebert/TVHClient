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
import org.tvheadend.data.entity.SeriesRecording
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [SeriesRecordingDataSource] against a real in-memory database.
 *
 * [Dispatchers.Unconfined] is injected as the `ioScope` so every write runs inline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SeriesRecordingDataSourceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppRoomDatabase
    private lateinit var dataSource: SeriesRecordingDataSource

    @Before
    fun setUp() {
        TestDatabase.plantNoopTimber()
        db = TestDatabase.create()
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        dataSource = SeriesRecordingDataSource(db, CoroutineScope(Dispatchers.Unconfined))
    }

    @After
    fun tearDown() {
        TestDatabase.teardown(db)
    }

    private fun seriesRecording(id: String, title: String? = id, start: Long = 0L, channelId: Int = 0): SeriesRecording {
        return SeriesRecording(id = id, title = title, start = start, channelId = channelId, connectionId = 1)
    }

    @Test
    fun addItemStoresTheRecording() {
        dataSource.addItem(seriesRecording("a"))

        assertThat(db.seriesRecordingDao.itemCountSync).isEqualTo(1)
        assertThat(db.seriesRecordingDao.loadRecordingByIdSync("a")!!.id).isEqualTo("a")
    }

    @Test
    fun updateItemReplacesTheStoredRecording() {
        dataSource.addItem(seriesRecording("a", title = "Before"))

        dataSource.updateItem(seriesRecording("a", title = "After"))

        assertThat(db.seriesRecordingDao.loadRecordingByIdSync("a")!!.title).isEqualTo("After")
    }

    @Test
    fun removeItemDeletesTheRecording() {
        dataSource.addItem(seriesRecording("a"))

        dataSource.removeItem(seriesRecording("a"))

        assertThat(db.seriesRecordingDao.itemCountSync).isEqualTo(0)
    }

    @Test
    fun getLiveDataItemCountReflectsTheStoredRecordings() {
        dataSource.addItem(seriesRecording("a"))
        dataSource.addItem(seriesRecording("b"))

        assertThat(dataSource.getLiveDataItemCount().observedValue()).isEqualTo(2)
    }

    @Test
    fun getLiveDataItemsReturnsTheMappedRecordings() {
        dataSource.addItem(seriesRecording("a", start = 100L))
        dataSource.addItem(seriesRecording("b", start = 50L))

        val items = dataSource.getLiveDataItems().observedValue()!!

        assertThat(items.map { it.id }).isEqualTo(listOf("b", "a"))
    }

    @Test
    fun getItemByIdReturnsTheRecordingOnlyForNonEmptyIds() {
        dataSource.addItem(seriesRecording("a", title = "One"))

        assertThat(dataSource.getItemById("a")!!.title).isEqualTo("One")
        assertThat(dataSource.getItemById("")).isNull()
        assertThat(dataSource.getItemById("missing")).isNull()
    }

    @Test
    fun getItemsReturnsAnEmptyList() {
        dataSource.addItem(seriesRecording("a"))

        assertThat(dataSource.getItems()).isEmpty()
    }
}
