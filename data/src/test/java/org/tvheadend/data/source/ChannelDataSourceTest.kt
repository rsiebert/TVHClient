package org.tvheadend.data.source

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ProgramEntity
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [ChannelDataSource] against a real in-memory database.
 *
 * The data source writes on a [CoroutineScope]. [Dispatchers.Unconfined] is injected so a
 * `launch` runs its body to completion inline: the DAO methods it calls are non-suspending, so
 * the very next suspension point never arrives. That makes every write deterministic with no
 * polling or sleeping.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChannelDataSourceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppRoomDatabase
    private lateinit var dataSource: ChannelDataSource

    @Before
    fun setUp() {
        TestDatabase.plantNoopTimber()
        db = TestDatabase.create()
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        dataSource = ChannelDataSource(db, CoroutineScope(Dispatchers.Unconfined))
    }

    @After
    fun tearDown() {
        TestDatabase.teardown(db)
    }

    @Test
    fun addItemStoresTheChannel() {
        dataSource.addItem(Channel(id = 1, connectionId = 1, name = "One"))

        assertThat(db.channelDao.itemCountSync).isEqualTo(1)
        assertThat(db.channelDao.loadChannelByIdSync(1)!!.name).isEqualTo("One")
    }

    @Test
    fun addItemsStoresAllChannels() {
        dataSource.addItems(listOf(
                Channel(id = 1, connectionId = 1, name = "One"),
                Channel(id = 2, connectionId = 1, name = "Two")))

        assertThat(db.channelDao.itemCountSync).isEqualTo(2)
    }

    @Test
    fun updateItemReplacesTheStoredChannel() {
        dataSource.addItem(Channel(id = 1, connectionId = 1, name = "One"))

        dataSource.updateItem(Channel(id = 1, connectionId = 1, name = "Uno"))

        assertThat(db.channelDao.loadChannelByIdSync(1)!!.name).isEqualTo("Uno")
        assertThat(db.channelDao.itemCountSync).isEqualTo(1)
    }

    @Test
    fun removeItemDeletesTheChannel() {
        dataSource.addItem(Channel(id = 1, connectionId = 1, name = "One"))

        dataSource.removeItem(Channel(id = 1, connectionId = 1, name = "One"))

        assertThat(db.channelDao.itemCountSync).isEqualTo(0)
    }

    @Test
    fun removeItemByIdDeletesTheChannel() {
        dataSource.addItem(Channel(id = 1, connectionId = 1, name = "One"))

        dataSource.removeItemById(1)

        assertThat(db.channelDao.itemCountSync).isEqualTo(0)
    }

    @Test
    fun getItemByIdReturnsTheStoredChannel() : Unit = runBlocking {
        dataSource.addItem(Channel(id = 1, connectionId = 1, name = "One"))

        val loaded = dataSource.getItemById(1)

        assertThat(loaded).isNotNull()
        assertThat(loaded!!.name).isEqualTo("One")
        assertThat(dataSource.getItemById(999)).isNull()
    }

    @Test
    fun getChannelsAppliesTheSortOrder() : Unit = runBlocking {
        dataSource.addItem(Channel(id = 1, connectionId = 1, name = "One", serverOrder = 2))
        dataSource.addItem(Channel(id = 2, connectionId = 1, name = "Two", serverOrder = 1))

        assertThat(dataSource.getChannels(sortOrder = 0).map { it.id }).isEqualTo(listOf(2, 1))
        assertThat(dataSource.getChannels(sortOrder = 2).map { it.id }).isEqualTo(listOf(1, 2))
    }

    @Test
    fun getItemsReturnsAllChannelsOfTheActiveConnection() : Unit = runBlocking {
        dataSource.addItem(Channel(id = 1, connectionId = 1, name = "One"))
        dataSource.addItem(Channel(id = 2, connectionId = 1, name = "Two"))

        assertThat(dataSource.getItems().map { it.id }).containsExactly(1, 2)
    }

    @Test
    fun getItemByIdWithProgramsReturnsTheChannelWithItsProgramColumns() : Unit = runBlocking {
        // The `channels` table has its own program_* columns that share the names of the aliases
        // in the query's join. Because those aliased join columns are now projected before `c.*`,
        // the joined program wins and the channel's stored (stale) columns are shadowed instead.
        dataSource.addItem(Channel(
                id = 1,
                connectionId = 1,
                name = "One",
                programId = 55,
                programTitle = "Cached title",
                programStart = 500L,
                programStop = 1_500L))
        // A matching program row exercises the LEFT JOIN of the query.
        db.programDao.insert(ProgramEntity(
                eventId = 100,
                channelId = 1,
                start = 0L,
                stop = 2_000L,
                title = "Joined title",
                connectionId = 1))

        val loaded = dataSource.getItemByIdWithPrograms(1, 1_000L)

        assertThat(loaded).isNotNull()
        // The program playing at :time (1_000) now wins over the channel's stored columns.
        assertThat(loaded!!.programTitle).isEqualTo("Joined title")
        assertThat(loaded.programStart).isEqualTo(0L)
        assertThat(loaded.programStop).isEqualTo(2_000L)
    }

    @Test
    fun getLiveDataItemCountReflectsTheStoredChannels() {
        dataSource.addItem(Channel(id = 1, connectionId = 1, name = "One"))
        dataSource.addItem(Channel(id = 2, connectionId = 1, name = "Two"))

        assertThat(dataSource.getLiveDataItemCount().observedValue()).isEqualTo(2)
    }
}
