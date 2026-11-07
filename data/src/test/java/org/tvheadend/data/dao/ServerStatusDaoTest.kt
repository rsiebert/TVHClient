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
import org.tvheadend.data.entity.ServerStatus
import org.tvheadend.data.entity.ServerStatusEntity
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [ServerStatusDao] against a real in-memory database.
 *
 * The `server_status` table is keyed by an auto-generated `id` and holds one row per connection.
 * The "active" queries select the row whose `connection_id` belongs to the active connection.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ServerStatusDaoTest {

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
    fun serverStatusCountOnlyCountsTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.serverStatusDao.insert(TestDatabase.serverStatus(connectionId = 1, id = 1, serverName = "Active"))
        db.serverStatusDao.insert(TestDatabase.serverStatus(connectionId = 2, id = 2, serverName = "Inactive"))

        assertThat(db.serverStatusDao.serverStatusCount.observedValue()).isEqualTo(1)
    }

    @Test
    fun loadAllServerStatusOnlyReturnsTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.serverStatusDao.insert(TestDatabase.serverStatus(connectionId = 1, id = 1, serverName = "Active"))
        db.serverStatusDao.insert(TestDatabase.serverStatus(connectionId = 2, id = 2, serverName = "Inactive"))

        val names = db.serverStatusDao.loadAllServerStatus().observedValue()!!.map { it.serverName }

        assertThat(names).containsExactly("Active")
    }

    @Test
    fun loadActiveServerStatusSyncReturnsTheActiveConnectionStatus() {
        insertActiveAndInactiveConnections()
        db.serverStatusDao.insert(TestDatabase.serverStatus(connectionId = 1, id = 1, serverName = "Active"))
        db.serverStatusDao.insert(TestDatabase.serverStatus(connectionId = 2, id = 2, serverName = "Inactive"))

        assertThat(db.serverStatusDao.loadActiveServerStatusSync()!!.serverName).isEqualTo("Active")
        assertThat(db.serverStatusDao.loadActiveServerStatus().observedValue()!!.serverName).isEqualTo("Active")
    }

    @Test
    fun loadActiveServerStatusSyncIsNullWhenNoneBelongsToTheActiveConnection() {
        insertActiveAndInactiveConnections()

        assertThat(db.serverStatusDao.loadActiveServerStatusSync()).isNull()
    }

    @Test
    fun loadServerStatusByIdFiltersByConnectionId() {
        insertActiveAndInactiveConnections()
        db.serverStatusDao.insert(TestDatabase.serverStatus(connectionId = 1, id = 1, serverName = "One"))
        db.serverStatusDao.insert(TestDatabase.serverStatus(connectionId = 2, id = 2, serverName = "Two"))

        assertThat(db.serverStatusDao.loadServerStatusByIdSync(1)!!.serverName).isEqualTo("One")
        assertThat(db.serverStatusDao.loadServerStatusById(2).observedValue()!!.serverName).isEqualTo("Two")
        assertThat(db.serverStatusDao.loadServerStatusByIdSync(999)).isNull()
    }

    @Test
    fun insertUpdateDeleteAndDeleteAll() {
        insertActiveAndInactiveConnections()
        db.serverStatusDao.insert(TestDatabase.serverStatus(connectionId = 1, id = 1, serverName = "Before"))
        assertThat(db.serverStatusDao.loadActiveServerStatusSync()!!.serverName).isEqualTo("Before")

        db.serverStatusDao.update(TestDatabase.serverStatus(connectionId = 1, id = 1, serverName = "After"))
        assertThat(db.serverStatusDao.loadActiveServerStatusSync()!!.serverName).isEqualTo("After")

        db.serverStatusDao.delete(TestDatabase.serverStatus(connectionId = 1, id = 1))
        assertThat(db.serverStatusDao.loadActiveServerStatusSync()).isNull()

        db.serverStatusDao.insert(TestDatabase.serverStatus(connectionId = 1, id = 5))
        db.serverStatusDao.deleteAll()
        assertThat(db.serverStatusDao.loadActiveServerStatusSync()).isNull()
    }

    @Test
    fun deleteByConnectionIdRemovesOnlyThatConnection() {
        insertActiveAndInactiveConnections()
        db.serverStatusDao.insert(TestDatabase.serverStatus(connectionId = 1, id = 1))
        db.serverStatusDao.insert(TestDatabase.serverStatus(connectionId = 2, id = 2))

        db.serverStatusDao.deleteByConnectionId(2)

        assertThat(db.serverStatusDao.loadServerStatusByIdSync(2)).isNull()
        assertThat(db.serverStatusDao.loadServerStatusByIdSync(1)).isNotNull()
    }

    @Test
    fun mapsServerStatusToEntityAndBack() {
        val serverStatus = ServerStatus(
                id = 1,
                connectionId = 7,
                connectionName = "Home",
                serverName = "Tvheadend",
                serverVersion = "4.3",
                htspVersion = 33,
                freeDiskSpace = 1_000L,
                totalDiskSpace = 2_000L,
                isAdmin = true,
                uiLevel = 2,
                uiLanguage = "en")

        val restored = ServerStatusEntity.from(serverStatus).toServerStatus()

        assertThat(restored).isEqualTo(serverStatus)
    }
}
