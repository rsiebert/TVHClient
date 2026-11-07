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
import org.tvheadend.data.entity.Program
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [ProgramDataSource] against a real in-memory database.
 *
 * The data source writes on a [CoroutineScope]. [Dispatchers.Unconfined] is injected so a `launch`
 * runs its body to completion inline: the DAO methods it calls are non-suspending, so the very next
 * suspension point never arrives. That makes every write deterministic with no polling or sleeping.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgramDataSourceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppRoomDatabase
    private lateinit var dataSource: ProgramDataSource

    @Before
    fun setUp() {
        TestDatabase.plantNoopTimber()
        db = TestDatabase.create()
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        dataSource = ProgramDataSource(db, CoroutineScope(Dispatchers.Unconfined))
    }

    @After
    fun tearDown() {
        TestDatabase.teardown(db)
    }

    private fun program(
            eventId: Int,
            channelId: Int = 0,
            start: Long = 0L,
            stop: Long = 1_000L,
            title: String? = "Program $eventId",
            subtitle: String? = null
    ): Program {
        return Program(
                eventId = eventId,
                channelId = channelId,
                start = start,
                stop = stop,
                title = title,
                subtitle = subtitle,
                connectionId = 1)
    }

    @Test
    fun addAndCountPrograms() {
        assertThat(dataSource.itemCount).isEqualTo(0)

        dataSource.addItem(program(eventId = 1))

        assertThat(dataSource.itemCount).isEqualTo(1)
        assertThat(db.programDao.loadProgramByIdSync(1)!!.title).isEqualTo("Program 1")
    }

    @Test
    fun addItemsStoresAllPrograms() {
        dataSource.addItems(listOf(program(eventId = 1), program(eventId = 2)))

        assertThat(dataSource.itemCount).isEqualTo(2)
    }

    @Test
    fun addItemsWithAnEmptyListStoresNothing() {
        dataSource.addItems(emptyList())

        assertThat(dataSource.itemCount).isEqualTo(0)
    }

    @Test
    fun updateItemReplacesTheStoredProgram() {
        dataSource.addItem(program(eventId = 1, title = "Before"))

        dataSource.updateItem(program(eventId = 1, title = "After"))

        assertThat(dataSource.itemCount).isEqualTo(1)
        assertThat(db.programDao.loadProgramByIdSync(1)!!.title).isEqualTo("After")
    }

    @Test
    fun removeItemDeletesTheProgram() {
        dataSource.addItem(program(eventId = 1))

        dataSource.removeItem(program(eventId = 1))

        assertThat(dataSource.itemCount).isEqualTo(0)
    }

    @Test
    fun removeItemsByTimeDeletesEndedPrograms() {
        dataSource.addItem(program(eventId = 1, stop = 1_000L))
        dataSource.addItem(program(eventId = 2, stop = 5_000L))

        dataSource.removeItemsByTime(3_000L)

        assertThat(db.programDao.loadProgramByIdSync(1)).isNull()
        assertThat(db.programDao.loadProgramByIdSync(2)).isNotNull()
    }

    @Test
    fun removeItemByIdDeletesTheProgram() {
        dataSource.addItem(program(eventId = 1))

        dataSource.removeItemById(1)

        assertThat(dataSource.itemCount).isEqualTo(0)
    }

    @Test
    fun getLiveDataItemCountReflectsTheStoredPrograms() {
        dataSource.addItem(program(eventId = 1))
        dataSource.addItem(program(eventId = 2))

        assertThat(dataSource.getLiveDataItemCount().observedValue()).isEqualTo(2)
    }

    @Test
    fun getLiveDataItemsReturnsTheMappedPrograms() {
        dataSource.addItem(program(eventId = 1, start = 100L))
        dataSource.addItem(program(eventId = 2, start = 200L))

        val items = dataSource.getLiveDataItems().observedValue()!!

        assertThat(items.map { it.eventId }).containsExactly(1, 2).inOrder()
    }

    @Test
    fun getLiveDataItemByIdReturnsTheProgram() {
        dataSource.addItem(program(eventId = 1, title = "One"))

        assertThat(dataSource.getLiveDataItemById(1).observedValue()!!.title).isEqualTo("One")
    }

    @Test
    fun getItemByIdReturnsTheProgramOrNull() {
        dataSource.addItem(program(eventId = 1, title = "One"))

        assertThat(dataSource.getItemById(1)!!.title).isEqualTo("One")
        assertThat(dataSource.getItemById(999)).isNull()
    }

    @Test
    fun getItemsReturnsEveryStoredProgram() {
        dataSource.addItem(program(eventId = 1))
        dataSource.addItem(program(eventId = 2))

        assertThat(dataSource.getItems().map { it.eventId }).containsExactly(1, 2)
    }

    @Test
    fun getLiveDataItemsFromTimeReturnsProgramsInTheTimeWindow() {
        dataSource.addItem(program(eventId = 1, start = 1_000L, stop = 2_000L))
        dataSource.addItem(program(eventId = 2, start = 3_000L, stop = 4_000L))

        val items = dataSource.getLiveDataItemsFromTime(2_500L).observedValue()!!

        assertThat(items.map { it.eventId }).containsExactly(2)
    }

    @Test
    fun getLiveDataItemByChannelIdAndTimeFiltersByChannel() {
        dataSource.addItem(program(eventId = 1, channelId = 100, start = 1_000L, stop = 2_000L))
        dataSource.addItem(program(eventId = 2, channelId = 200, start = 1_000L, stop = 2_000L))

        val items = dataSource.getLiveDataItemByChannelIdAndTime(100, 500L).observedValue()!!

        assertThat(items.map { it.eventId }).containsExactly(1)
    }

    @Test
    fun getItemByChannelIdAndBetweenTimeReturnsOverlappingPrograms() {
        dataSource.addItem(program(eventId = 1, channelId = 100, start = 2_500L, stop = 3_500L))
        dataSource.addItem(program(eventId = 2, channelId = 100, start = 100L, stop = 500L))

        val items = dataSource.getItemByChannelIdAndBetweenTime(100, 2_000L, 4_000L)

        assertThat(items.map { it.eventId }).containsExactly(1)
    }

    @Test
    fun getLastItemByChannelIdReturnsTheLatestProgram() {
        dataSource.addItem(program(eventId = 1, channelId = 100, start = 100L))
        dataSource.addItem(program(eventId = 2, channelId = 100, start = 300L))

        assertThat(dataSource.getLastItemByChannelId(100)!!.eventId).isEqualTo(2)
        assertThat(dataSource.getLastItemByChannelId(999)).isNull()
    }

    @Test
    fun getItemsByChannelIdReturnsProgramsFromNowUntilTheLastProgram() {
        val now = System.currentTimeMillis()
        dataSource.addItem(program(eventId = 1, channelId = 100, start = now + 100_000L, stop = now + 200_000L))

        val items = dataSource.getItemsByChannelId(100)

        assertThat(items.map { it.eventId }).containsExactly(1)
    }

    @Test
    fun getDuplicateProgramsGroupsByTitleAndSubtitle() {
        dataSource.addItem(program(eventId = 1, channelId = 100, title = "Show", subtitle = "S1"))
        dataSource.addItem(program(eventId = 2, channelId = 100, title = "Show", subtitle = "S1"))
        dataSource.addItem(program(eventId = 3, channelId = 100, title = "Show", subtitle = "S2"))

        val duplicates = dataSource.getDuplicatePrograms(100)

        assertThat(duplicates.map { it.subtitle }).containsExactly("S1", "S2")
    }
}
