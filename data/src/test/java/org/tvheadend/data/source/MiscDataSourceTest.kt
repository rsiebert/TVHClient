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
import org.tvheadend.data.testutil.TestDatabase

/**
 * Robolectric test for [MiscDataSource] against a real in-memory database.
 *
 * [Dispatchers.Unconfined] is injected as the `ioScope` so the callback runs inline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MiscDataSourceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppRoomDatabase
    private lateinit var dataSource: MiscDataSource

    @Before
    fun setUp() {
        TestDatabase.plantNoopTimber()
        db = TestDatabase.create()
        dataSource = MiscDataSource(db, CoroutineScope(Dispatchers.Unconfined))
    }

    @After
    fun tearDown() {
        TestDatabase.teardown(db)
    }

    private fun tagAndChannelCount(): Int {
        db.query("SELECT COUNT(*) FROM tags_and_channels", null).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }

    private fun seedDatabase() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1).copy(lastUpdate = 500L, isSyncRequired = false))
        db.channelDao.insert(TestDatabase.channel(id = 10, connectionId = 1))
        db.channelTagDao.insert(TestDatabase.channelTag(tagId = 1, connectionId = 1))
        db.tagAndChannelDao.insert(TestDatabase.tagAndChannel(tagId = 1, channelId = 10, connectionId = 1))
        db.programDao.insert(TestDatabase.program(eventId = 1, connectionId = 1))
        db.recordingDao.insert(TestDatabase.recording(id = 1, connectionId = 1))
        db.seriesRecordingDao.insert(TestDatabase.seriesRecording(id = "a", connectionId = 1))
        db.timerRecordingDao.insert(TestDatabase.timerRecording(id = "a", connectionId = 1))
        db.serverProfileDao.insert(TestDatabase.serverProfile(connectionId = 1, id = 1))
        val serverStatus = TestDatabase.serverStatus(connectionId = 1, id = 1)
        serverStatus.htspPlaybackServerProfileId = 10
        serverStatus.recordingServerProfileId = 20
        db.serverStatusDao.insert(serverStatus)
    }

    @Test
    fun clearDatabaseEmptiesEveryTableAndInvokesTheCallback() {
        seedDatabase()
        var callbackInvoked = false
        val callback = object : MiscDataSource.DatabaseClearedCallback {
            override fun onDatabaseCleared() {
                callbackInvoked = true
            }
        }

        dataSource.clearDatabase(callback)

        assertThat(callbackInvoked).isTrue()
        assertThat(db.channelDao.itemCountSync).isEqualTo(0)
        assertThat(db.channelTagDao.itemCountSync).isEqualTo(0)
        assertThat(db.programDao.itemCountSync).isEqualTo(0)
        assertThat(db.recordingDao.itemCountSync).isEqualTo(0)
        assertThat(db.seriesRecordingDao.itemCountSync).isEqualTo(0)
        assertThat(db.timerRecordingDao.itemCountSync).isEqualTo(0)
        assertThat(db.serverProfileDao.itemCountSync).isEqualTo(0)
        assertThat(tagAndChannelCount()).isEqualTo(0)
    }

    @Test
    fun clearDatabaseResetsTheConnectionSyncFlagsAndProfileAssignments() {
        seedDatabase()

        dataSource.clearDatabase(object : MiscDataSource.DatabaseClearedCallback {
            override fun onDatabaseCleared() = Unit
        })

        val connection = db.connectionDao.loadConnectionByIdSync(1)!!
        assertThat(connection.lastUpdate).isEqualTo(0L)
        assertThat(connection.isSyncRequired).isTrue()

        val serverStatus = db.serverStatusDao.loadServerStatusByIdSync(1)!!
        assertThat(serverStatus.htspPlaybackServerProfileId).isEqualTo(0)
        assertThat(serverStatus.recordingServerProfileId).isEqualTo(0)
    }

    @Test
    fun clearDatabaseToleratesAConnectionWithoutServerStatus() {
        // A connection without a server status row: the reset loop must skip it without crashing.
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))

        dataSource.clearDatabase(object : MiscDataSource.DatabaseClearedCallback {
            override fun onDatabaseCleared() = Unit
        })

        assertThat(db.connectionDao.loadConnectionByIdSync(1)!!.isSyncRequired).isTrue()
    }
}
