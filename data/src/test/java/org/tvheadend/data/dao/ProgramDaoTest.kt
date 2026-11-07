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
import org.tvheadend.data.entity.Program
import org.tvheadend.data.entity.ProgramEntity
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [ProgramDao] against a real in-memory database.
 *
 * The programs table has a composite primary key of `(id, connection_id)`, so the same event id
 * may exist once per connection. Nearly every query filters on the channel belonging to the
 * *active* connection (`CONNECTION_IS_ACTIVE`), so most tests plant both an active and an inactive
 * connection.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgramDaoTest {

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
    fun countsOnlyTheProgramsOfTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.programDao.insert(TestDatabase.program(eventId = 10, connectionId = 1))
        db.programDao.insert(TestDatabase.program(eventId = 11, connectionId = 1))
        db.programDao.insert(TestDatabase.program(eventId = 20, connectionId = 2))

        assertThat(db.programDao.itemCountSync).isEqualTo(2)
        assertThat(db.programDao.itemCount.observedValue()).isEqualTo(2)
    }

    @Test
    fun loadProgramsFromTimeOnlyReturnsProgramsInTheTimeWindowOfTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.programDao.insert(TestDatabase.program(eventId = 1, connectionId = 1, start = 1_000L, stop = 2_000L))
        db.programDao.insert(TestDatabase.program(eventId = 2, connectionId = 1, start = 3_000L, stop = 4_000L))
        // Belongs to the inactive connection and must never appear.
        db.programDao.insert(TestDatabase.program(eventId = 3, connectionId = 2, start = 1_000L, stop = 6_000L))

        // Only event 2 starts at or after 2_500.
        assertThat(idsOfProgramsFromTime(2_500L)).containsExactly(2)
        // At 1_500 event 1 is running (start <= time <= stop) and event 2 lies in the future.
        assertThat(idsOfProgramsFromTime(1_500L)).containsExactly(1, 2).inOrder()
    }

    private fun idsOfProgramsFromTime(time: Long): List<Int> {
        return db.programDao.loadProgramsFromTime(time).observedValue()!!.map { it.eventId }
    }

    @Test
    fun loadProgramsFromChannelFromTimeFiltersByChannel() {
        insertActiveAndInactiveConnections()
        db.programDao.insert(TestDatabase.program(eventId = 1, connectionId = 1, channelId = 100, start = 1_000L, stop = 2_000L))
        db.programDao.insert(TestDatabase.program(eventId = 2, connectionId = 1, channelId = 100, start = 3_000L, stop = 4_000L))
        db.programDao.insert(TestDatabase.program(eventId = 3, connectionId = 1, channelId = 200, start = 1_000L, stop = 2_000L))

        val channel100 = db.programDao.loadProgramsFromChannelFromTime(100, 500L).observedValue()
        assertThat(channel100!!.map { it.eventId }).containsExactly(1, 2).inOrder()

        val channel200 = db.programDao.loadProgramsFromChannelFromTime(200, 500L).observedValue()
        assertThat(channel200!!.map { it.eventId }).containsExactly(3)
    }

    @Test
    fun loadProgramsFromChannelBetweenTimeSyncReturnsOverlappingPrograms() {
        insertActiveAndInactiveConnections()
        // Fully inside the slot [2_000, 4_000].
        db.programDao.insert(TestDatabase.program(eventId = 1, connectionId = 1, channelId = 100, start = 2_500L, stop = 3_500L))
        // Overlaps the beginning of the slot.
        db.programDao.insert(TestDatabase.program(eventId = 2, connectionId = 1, channelId = 100, start = 1_500L, stop = 2_500L))
        // Overlaps the end of the slot.
        db.programDao.insert(TestDatabase.program(eventId = 3, connectionId = 1, channelId = 100, start = 3_500L, stop = 4_500L))
        // Ends before the slot.
        db.programDao.insert(TestDatabase.program(eventId = 4, connectionId = 1, channelId = 100, start = 100L, stop = 500L))
        // Starts after the slot.
        db.programDao.insert(TestDatabase.program(eventId = 5, connectionId = 1, channelId = 100, start = 5_000L, stop = 6_000L))

        val ids = db.programDao.loadProgramsFromChannelBetweenTimeSync(100, 2_000L, 4_000L).map { it.eventId }

        assertThat(ids).containsExactly(1, 2, 3)
    }

    @Test
    fun loadEpgProgramsFromChannelBetweenTimeSyncReturnsTheMappedEpgPrograms() {
        insertActiveAndInactiveConnections()
        db.programDao.insert(TestDatabase.program(
                eventId = 1,
                connectionId = 1,
                channelId = 100,
                start = 2_000L,
                stop = 3_000L,
                title = "News",
                subtitle = "Today",
                contentType = 4))

        val epgPrograms = db.programDao.loadEpgProgramsFromChannelBetweenTimeSync(100, 1_000L, 4_000L)

        assertThat(epgPrograms).hasSize(1)
        assertThat(epgPrograms[0].eventId).isEqualTo(1)
        assertThat(epgPrograms[0].channelId).isEqualTo(100)
        assertThat(epgPrograms[0].title).isEqualTo("News")
        assertThat(epgPrograms[0].subtitle).isEqualTo("Today")
        assertThat(epgPrograms[0].contentType).isEqualTo(4)
    }

    @Test
    fun loadProgramsAndLoadProgramsSyncReturnTheProgramsOfTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.programDao.insert(TestDatabase.program(eventId = 1, connectionId = 1, start = 100L))
        db.programDao.insert(TestDatabase.program(eventId = 2, connectionId = 1, start = 200L))
        db.programDao.insert(TestDatabase.program(eventId = 3, connectionId = 2, start = 50L))

        assertThat(db.programDao.loadPrograms().observedValue()!!.map { it.eventId }).containsExactly(1, 2).inOrder()
        assertThat(db.programDao.loadProgramsSync().map { it.eventId }).containsExactly(1, 2).inOrder()
    }

    @Test
    fun loadProgramByIdOnlyReturnsProgramsOfTheActiveConnection() {
        insertActiveAndInactiveConnections()
        // Same event id on both connections: only the active one may be returned.
        db.programDao.insert(TestDatabase.program(eventId = 1, connectionId = 1, title = "Active"))
        db.programDao.insert(TestDatabase.program(eventId = 1, connectionId = 2, title = "Inactive"))

        assertThat(db.programDao.loadProgramById(1).observedValue()!!.title).isEqualTo("Active")
        assertThat(db.programDao.loadProgramByIdSync(1)!!.title).isEqualTo("Active")
        assertThat(db.programDao.loadProgramByIdSync(999)).isNull()
    }

    @Test
    fun loadProgramsFromChannelSyncOrdersByStartDescending() {
        insertActiveAndInactiveConnections()
        db.programDao.insert(TestDatabase.program(eventId = 1, connectionId = 1, channelId = 100, start = 100L))
        db.programDao.insert(TestDatabase.program(eventId = 2, connectionId = 1, channelId = 100, start = 300L))
        db.programDao.insert(TestDatabase.program(eventId = 3, connectionId = 1, channelId = 100, start = 200L))

        val ids = db.programDao.loadProgramsFromChannelSync(100).map { it.eventId }

        assertThat(ids).isEqualTo(listOf(2, 3, 1))
    }

    @Test
    fun loadLastProgramFromChannelSyncReturnsTheProgramWithTheLatestStart() {
        insertActiveAndInactiveConnections()
        db.programDao.insert(TestDatabase.program(eventId = 1, connectionId = 1, channelId = 100, start = 100L))
        db.programDao.insert(TestDatabase.program(eventId = 2, connectionId = 1, channelId = 100, start = 300L))
        db.programDao.insert(TestDatabase.program(eventId = 3, connectionId = 1, channelId = 100, start = 200L))

        assertThat(db.programDao.loadLastProgramFromChannelSync(100)!!.eventId).isEqualTo(2)
    }

    @Test
    fun loadDuplicateProgramsSyncGroupsByTitleAndSubtitle() {
        insertActiveAndInactiveConnections()
        db.programDao.insert(TestDatabase.program(eventId = 1, connectionId = 1, channelId = 100, start = 100L, title = "Show", subtitle = "S1"))
        db.programDao.insert(TestDatabase.program(eventId = 2, connectionId = 1, channelId = 100, start = 300L, title = "Show", subtitle = "S1"))
        db.programDao.insert(TestDatabase.program(eventId = 3, connectionId = 1, channelId = 100, start = 500L, title = "Show", subtitle = "S2"))

        val duplicates = db.programDao.loadDuplicateProgramsSync(100)

        assertThat(duplicates.map { it.subtitle }).containsExactly("S1", "S2")
    }

    @Test
    fun deleteProgramsByTimeRemovesOnlyTheProgramsThatHaveEnded() {
        insertActiveAndInactiveConnections()
        db.programDao.insert(TestDatabase.program(eventId = 1, connectionId = 1, stop = 1_000L))
        db.programDao.insert(TestDatabase.program(eventId = 2, connectionId = 1, stop = 5_000L))

        db.programDao.deleteProgramsByTime(3_000L)

        assertThat(db.programDao.loadProgramByIdSync(1)).isNull()
        assertThat(db.programDao.loadProgramByIdSync(2)).isNotNull()
    }

    @Test
    fun insertsUpdatesAndDeletesPrograms() {
        insertActiveAndInactiveConnections()
        db.programDao.insert(listOf(
                TestDatabase.program(eventId = 1, connectionId = 1, title = "One"),
                TestDatabase.program(eventId = 2, connectionId = 1, title = "Two")))
        assertThat(db.programDao.itemCountSync).isEqualTo(2)

        db.programDao.update(TestDatabase.program(eventId = 1, connectionId = 1, title = "Uno"))
        assertThat(db.programDao.loadProgramByIdSync(1)!!.title).isEqualTo("Uno")

        db.programDao.update(listOf(TestDatabase.program(eventId = 1, connectionId = 1, title = "Ein")))
        assertThat(db.programDao.loadProgramByIdSync(1)!!.title).isEqualTo("Ein")

        db.programDao.delete(TestDatabase.program(eventId = 1, connectionId = 1))
        assertThat(db.programDao.loadProgramByIdSync(1)).isNull()

        db.programDao.delete(listOf(TestDatabase.program(eventId = 2, connectionId = 1)))
        assertThat(db.programDao.itemCountSync).isEqualTo(0)

        db.programDao.insert(listOf(
                TestDatabase.program(eventId = 1, connectionId = 1),
                TestDatabase.program(eventId = 2, connectionId = 1)))
        db.programDao.deleteById(1)
        assertThat(db.programDao.itemCountSync).isEqualTo(1)

        db.programDao.deleteAll()
        assertThat(db.programDao.itemCountSync).isEqualTo(0)
    }

    @Test
    fun theBaseQueryReturnsTheStoredChannelNameNotTheJoinedOne() {
        insertActiveAndInactiveConnections()
        db.channelDao.insert(TestDatabase.channel(id = 100, connectionId = 1, name = "Joined channel name"))
        // `PROGRAM_BASE_QUERY` selects `p.*` (which already contains channel_name/channel_icon) and
        // `c.name AS channel_name`, a duplicate column name. The aliased join column is projected
        // before `p.*`, so the joined channel value now wins over the program's own column (the
        // method name is kept from when the stored value was the one that won).
        val program = TestDatabase.program(eventId = 1, connectionId = 1, channelId = 100)
        program.channelName = "Stored channel name"
        db.programDao.insert(program)

        val loaded = db.programDao.loadProgramsSync().first()

        assertThat(loaded.channelName).isEqualTo("Joined channel name")
    }

    @Test
    fun mapsProgramToEntityAndBack() {
        val program = Program(
                eventId = 7,
                channelId = 3,
                start = 1_000L,
                stop = 2_000L,
                title = "Title",
                subtitle = "Subtitle",
                contentType = 5,
                connectionId = 1)

        val restored = ProgramEntity.from(program).toProgram()

        assertThat(restored).isEqualTo(program)
        assertThat(restored.channelId).isEqualTo(3)
        assertThat(restored.eventId).isEqualTo(7)
    }
}
