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
import org.tvheadend.data.entity.ConnectionEntity
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [ConnectionDao] against a real in-memory database.
 *
 * Connections are the only table *without* an active-connection filter: the active connection is
 * simply the single row with `active = 1`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConnectionDaoTest {

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

    @Test
    fun connectionCountCountsAllConnections() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        db.connectionDao.insert(TestDatabase.inactiveConnection(id = 2))

        assertThat(db.connectionDao.connectionCount.observedValue()).isEqualTo(2)
    }

    @Test
    fun loadAllConnectionsReturnsEveryConnection() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1).copy(name = "Active"))
        db.connectionDao.insert(TestDatabase.inactiveConnection(id = 2).copy(name = "Inactive"))

        val liveDataIds = db.connectionDao.loadAllConnections().observedValue()!!.map { it.id }
        val syncIds = db.connectionDao.loadAllConnectionsSync().map { it.id }

        assertThat(liveDataIds).containsExactly(1, 2)
        assertThat(syncIds).containsExactly(1, 2)
    }

    @Test
    fun loadActiveConnectionReturnsOnlyTheActiveOne() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        db.connectionDao.insert(TestDatabase.inactiveConnection(id = 2))

        assertThat(db.connectionDao.loadActiveConnectionSync()!!.id).isEqualTo(1)
        assertThat(db.connectionDao.loadActiveConnection().observedValue()!!.id).isEqualTo(1)
    }

    @Test
    fun loadActiveConnectionIsNullWhenNoConnectionIsActive() {
        db.connectionDao.insert(TestDatabase.inactiveConnection(id = 2))

        assertThat(db.connectionDao.loadActiveConnectionSync()).isNull()
        assertThat(db.connectionDao.loadActiveConnection().observedValue()).isNull()
    }

    @Test
    fun loadConnectionByIdReturnsTheRequestedConnection() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        db.connectionDao.insert(TestDatabase.inactiveConnection(id = 2))

        assertThat(db.connectionDao.loadConnectionByIdSync(2)!!.name).isEqualTo("Inactive")
        assertThat(db.connectionDao.loadConnectionById(2).observedValue()!!.id).isEqualTo(2)
        assertThat(db.connectionDao.loadConnectionByIdSync(999)).isNull()
    }

    @Test
    fun insertReturnsTheGeneratedRowId() {
        val id = db.connectionDao.insert(ConnectionEntity(name = "New"))

        assertThat(id).isEqualTo(1L)
        assertThat(db.connectionDao.loadConnectionByIdSync(1)).isNotNull()
    }

    @Test
    fun updateChangesTheStoredConnection() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))

        db.connectionDao.update(TestDatabase.activeConnection(id = 1).copy(name = "Renamed"))

        assertThat(db.connectionDao.loadConnectionByIdSync(1)!!.name).isEqualTo("Renamed")
    }

    @Test
    fun deleteRemovesTheConnection() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))

        db.connectionDao.delete(TestDatabase.activeConnection(id = 1))

        assertThat(db.connectionDao.connectionCount.observedValue()).isEqualTo(0)
    }

    @Test
    fun disableActiveConnectionClearsTheActiveFlag() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        db.connectionDao.insert(TestDatabase.inactiveConnection(id = 2))

        db.connectionDao.disableActiveConnection()

        assertThat(db.connectionDao.loadActiveConnectionSync()).isNull()
        assertThat(db.connectionDao.loadConnectionByIdSync(2)!!.isActive).isFalse()
    }
}
