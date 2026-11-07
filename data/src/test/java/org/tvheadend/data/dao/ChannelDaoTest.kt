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
import org.tvheadend.data.entity.ChannelTagEntity
import org.tvheadend.data.entity.ProgramEntity
import org.tvheadend.data.entity.TagAndChannelEntity
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [ChannelDao] against a real in-memory database.
 *
 * The queries almost all filter on the channel belonging to the *active* connection
 * (`CONNECTION_IS_ACTIVE`), so most tests plant both an active and an inactive connection.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChannelDaoTest {

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
    fun countsOnlyTheChannelsOfTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.channelDao.insert(TestDatabase.channel(id = 10, connectionId = 1))
        db.channelDao.insert(TestDatabase.channel(id = 11, connectionId = 1))
        db.channelDao.insert(TestDatabase.channel(id = 20, connectionId = 2))

        assertThat(db.channelDao.itemCountSync).isEqualTo(2)
        assertThat(db.channelDao.itemCount.observedValue()).isEqualTo(2)
    }

    @Test
    fun loadChannelByIdOnlyReturnsChannelsOfTheActiveConnection() {
        insertActiveAndInactiveConnections()
        // Same id on both connections: only the active one may be returned.
        db.channelDao.insert(TestDatabase.channel(id = 10, connectionId = 1, name = "Active channel"))
        db.channelDao.insert(TestDatabase.channel(id = 10, connectionId = 2, name = "Inactive channel"))

        val loaded = db.channelDao.loadChannelByIdSync(10)

        assertThat(loaded).isNotNull()
        assertThat(loaded!!.name).isEqualTo("Active channel")
        assertThat(db.channelDao.loadChannelByIdSync(999)).isNull()
    }

    @Test
    fun loadChannelByIdWithProgramsReturnsTheChannelWithItsProgramColumns() {
        insertActiveAndInactiveConnections()
        // The `channels` table has its own program_* columns that share the names of the aliases
        // in CHANNEL_BASE_QUERY's join. Because those aliased join columns are now projected
        // before `c.*`, the joined program wins and the channel's stored (stale) columns are
        // shadowed instead.
        val channel = TestDatabase.channel(id = 10, connectionId = 1, name = "One").apply {
            programId = 55
            programTitle = "Cached title"
            programStart = 500L
            programStop = 1_500L
        }
        db.channelDao.insert(channel)
        // A matching program row exercises the LEFT JOIN of the query.
        db.programDao.insert(ProgramEntity(
                eventId = 100,
                channelId = 10,
                start = 0L,
                stop = 2_000L,
                title = "Joined title",
                connectionId = 1))

        val loaded = db.channelDao.loadChannelByIdWithProgramsSync(10, 1_000L)

        assertThat(loaded).isNotNull()
        assertThat(loaded!!.name).isEqualTo("One")
        // The program playing at :time (1_000) now wins over the channel's stored columns.
        assertThat(loaded.programId).isEqualTo(100)
        assertThat(loaded.programTitle).isEqualTo("Joined title")
        assertThat(loaded.programStart).isEqualTo(0L)
        assertThat(loaded.programStop).isEqualTo(2_000L)
    }

    @Test
    fun loadChannelByIdWithProgramsIsNullWhenNoProgramIsPlaying() {
        insertActiveAndInactiveConnections()
        db.channelDao.insert(TestDatabase.channel(id = 10, connectionId = 1))

        // Channel exists, the join is a LEFT JOIN so the channel is still returned without a program.
        val loaded = db.channelDao.loadChannelByIdWithProgramsSync(10, 1_000L)

        assertThat(loaded).isNotNull()
        assertThat(loaded!!.programId).isEqualTo(0)
    }

    @Test
    fun loadAllChannelsAppliesTheRequestedSortOrder() {
        insertActiveAndInactiveConnections()
        // server_order, name and display_number deliberately disagree so each sort is verifiable.
        db.channelDao.insert(TestDatabase.channel(id = 1, connectionId = 1, name = "Charlie", displayNumber = "20", serverOrder = 1))
        db.channelDao.insert(TestDatabase.channel(id = 2, connectionId = 1, name = "Alpha", displayNumber = "30", serverOrder = 3))
        db.channelDao.insert(TestDatabase.channel(id = 3, connectionId = 1, name = "Bravo", displayNumber = "10", serverOrder = 2))
        // A channel of the inactive connection must never appear.
        db.channelDao.insert(TestDatabase.channel(id = 4, connectionId = 2, name = "Hidden", displayNumber = "1", serverOrder = 0))

        assertThat(idsForSortOrder(0)).isEqualTo(listOf(1, 3, 2))
        assertThat(idsForSortOrder(1)).isEqualTo(listOf(2, 3, 1))
        assertThat(idsForSortOrder(2)).isEqualTo(listOf(1, 2, 3))
        assertThat(idsForSortOrder(4)).isEqualTo(listOf(2, 3, 1))
        assertThat(idsForSortOrder(6)).isEqualTo(listOf(3, 1, 2))
    }

    private fun idsForSortOrder(sortOrder: Int): List<Int> {
        return db.channelDao.loadAllChannelsSync(sortOrder).map { it.id }
    }

    @Test
    fun insertsUpdatesAndDeletesChannels() {
        insertActiveAndInactiveConnections()
        db.channelDao.insert(listOf(
                TestDatabase.channel(id = 1, connectionId = 1, name = "One"),
                TestDatabase.channel(id = 2, connectionId = 1, name = "Two")))
        assertThat(db.channelDao.itemCountSync).isEqualTo(2)

        val updated = TestDatabase.channel(id = 1, connectionId = 1, name = "Uno")
        db.channelDao.update(updated)
        assertThat(db.channelDao.loadChannelByIdSync(1)!!.name).isEqualTo("Uno")

        db.channelDao.delete(updated)
        assertThat(db.channelDao.loadChannelByIdSync(1)).isNull()

        db.channelDao.deleteById(2)
        assertThat(db.channelDao.itemCountSync).isEqualTo(0)

        db.channelDao.insert(TestDatabase.channel(id = 3, connectionId = 1))
        db.channelDao.deleteAll()
        assertThat(db.channelDao.itemCountSync).isEqualTo(0)
    }

    @Test
    fun loadAllEpgChannelsReturnsOnlyTheChannelsOfTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.channelDao.insert(TestDatabase.channel(id = 1, connectionId = 1, name = "One"))
        db.channelDao.insert(TestDatabase.channel(id = 2, connectionId = 1, name = "Two"))
        db.channelDao.insert(TestDatabase.channel(id = 3, connectionId = 2, name = "Hidden"))

        val epgChannels = db.channelDao.loadAllEpgChannels(0).observedValue()

        assertThat(epgChannels!!.map { it.id }).containsExactly(1, 2)
    }

    @Test
    fun loadAllEpgChannelsByTagReturnsOnlyTheTaggedChannels() {
        insertActiveAndInactiveConnections()
        db.channelDao.insert(TestDatabase.channel(id = 1, connectionId = 1, name = "One"))
        db.channelDao.insert(TestDatabase.channel(id = 2, connectionId = 1, name = "Two"))
        db.channelTagDao.insert(ChannelTagEntity(tagId = 5, tagName = "News", connectionId = 1))
        db.tagAndChannelDao.insert(TagAndChannelEntity(tagId = 5, channelId = 1, connectionId = 1))

        val epgChannels = db.channelDao.loadAllEpgChannelsByTag(0, listOf(5)).observedValue()

        assertThat(epgChannels!!.map { it.id }).containsExactly(1)
    }
}
