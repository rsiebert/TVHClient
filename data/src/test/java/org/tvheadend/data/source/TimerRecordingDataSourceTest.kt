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
import org.tvheadend.data.entity.TimerRecording
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [TimerRecordingDataSource] against a real in-memory database.
 *
 * [Dispatchers.Unconfined] is injected as the `ioScope` so every write runs inline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimerRecordingDataSourceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppRoomDatabase
    private lateinit var dataSource: TimerRecordingDataSource

    @Before
    fun setUp() {
        TestDatabase.plantNoopTimber()
        db = TestDatabase.create()
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        dataSource = TimerRecordingDataSource(db, CoroutineScope(Dispatchers.Unconfined))
    }

    @After
    fun tearDown() {
        TestDatabase.teardown(db)
    }

    private fun timerRecording(id: String, title: String? = id, start: Long = 0L, channelId: Int = 0): TimerRecording {
        return TimerRecording(id = id, title = title, start = start, channelId = channelId, connectionId = 1)
    }

    @Test
    fun addItemStoresTheRecording() {
        dataSource.addItem(timerRecording("a"))

        assertThat(db.timerRecordingDao.itemCountSync).isEqualTo(1)
        assertThat(db.timerRecordingDao.loadRecordingByIdSync("a")!!.id).isEqualTo("a")
    }

    @Test
    fun updateItemReplacesTheStoredRecording() {
        dataSource.addItem(timerRecording("a", title = "Before"))

        dataSource.updateItem(timerRecording("a", title = "After"))

        assertThat(db.timerRecordingDao.loadRecordingByIdSync("a")!!.title).isEqualTo("After")
    }

    @Test
    fun removeItemDeletesTheRecording() {
        dataSource.addItem(timerRecording("a"))

        dataSource.removeItem(timerRecording("a"))

        assertThat(db.timerRecordingDao.itemCountSync).isEqualTo(0)
    }

    @Test
    fun getLiveDataItemCountReflectsTheStoredRecordings() {
        dataSource.addItem(timerRecording("a"))
        dataSource.addItem(timerRecording("b"))

        assertThat(dataSource.getLiveDataItemCount().observedValue()).isEqualTo(2)
    }

    @Test
    fun getLiveDataItemsReturnsTheMappedRecordings() {
        dataSource.addItem(timerRecording("a", start = 100L))
        dataSource.addItem(timerRecording("b", start = 50L))

        val items = dataSource.getLiveDataItems().observedValue()!!

        assertThat(items.map { it.id }).isEqualTo(listOf("b", "a"))
    }

    @Test
    fun getLiveDataItemByIdReturnsTheRecording() {
        dataSource.addItem(timerRecording("a", title = "One"))

        assertThat(dataSource.getLiveDataItemById("a").observedValue()!!.title).isEqualTo("One")
    }

    @Test
    fun getItemByIdReturnsTheRecordingOnlyForNonEmptyIds() {
        dataSource.addItem(timerRecording("a", title = "One"))

        assertThat(dataSource.getItemById("a")!!.title).isEqualTo("One")
        assertThat(dataSource.getItemById("")).isNull()
        assertThat(dataSource.getItemById("missing")).isNull()
    }

    @Test
    fun getItemsReturnsAnEmptyList() {
        dataSource.addItem(timerRecording("a"))

        assertThat(dataSource.getItems()).isEmpty()
    }
}
