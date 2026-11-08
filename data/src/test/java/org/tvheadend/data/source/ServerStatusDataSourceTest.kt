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
import org.tvheadend.data.entity.ServerStatus
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [ServerStatusDataSource] against a real in-memory database.
 *
 * [Dispatchers.Unconfined] is injected as the `ioScope` so every write runs inline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ServerStatusDataSourceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppRoomDatabase
    private lateinit var dataSource: ServerStatusDataSource

    @Before
    fun setUp() {
        TestDatabase.plantNoopTimber()
        db = TestDatabase.create()
        dataSource = ServerStatusDataSource(db, CoroutineScope(Dispatchers.Unconfined))
    }

    @After
    fun tearDown() {
        TestDatabase.teardown(db)
    }

    private fun serverStatus(connectionId: Int, id: Int = 0, name: String? = null): ServerStatus {
        return ServerStatus(id = id, connectionId = connectionId, serverName = name)
    }

    @Test
    fun activeItemInsertsADefaultWhenNoneExists() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))

        val status = dataSource.activeItem

        assertThat(status.serverName).isEqualTo("Unknown")
        assertThat(status.serverVersion).isEqualTo("Unknown")
        assertThat(status.connectionId).isEqualTo(1)
        assertThat(status.connectionName).isEqualTo("Active")
        assertThat(db.serverStatusDao.loadActiveServerStatusSync()).isNotNull()
    }

    @Test
    fun activeItemReturnsTheStoredStatus() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        db.serverStatusDao.insert(TestDatabase.serverStatus(connectionId = 1, id = 1, serverName = "Tvheadend", serverVersion = "4.3"))

        val status = dataSource.activeItem

        assertThat(status.serverName).isEqualTo("Tvheadend")
        assertThat(status.serverVersion).isEqualTo("4.3")
    }

    @Test
    fun activeItemWithNoActiveConnectionReturnsUnknownsWithoutInserting() {
        val status = dataSource.activeItem

        assertThat(status.serverName).isEqualTo("Unknown")
        assertThat(status.connectionId).isEqualTo(0)
        assertThat(db.serverStatusDao.serverStatusCount.observedValue()).isEqualTo(0)
    }

    @Test
    fun addUpdateAndRemoveItem() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))

        dataSource.addItem(serverStatus(connectionId = 1, id = 1, name = "Before"))
        assertThat(db.serverStatusDao.loadActiveServerStatusSync()!!.serverName).isEqualTo("Before")

        dataSource.updateItem(serverStatus(connectionId = 1, id = 1, name = "After"))
        assertThat(db.serverStatusDao.loadActiveServerStatusSync()!!.serverName).isEqualTo("After")

        dataSource.removeItem(serverStatus(connectionId = 1, id = 1))
        assertThat(db.serverStatusDao.loadActiveServerStatusSync()).isNull()
    }

    @Test
    fun getLiveDataItemCountReflectsTheActiveConnection() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        dataSource.addItem(serverStatus(connectionId = 1, id = 1))

        assertThat(dataSource.getLiveDataItemCount().observedValue()).isEqualTo(1)
    }

    @Test
    fun getLiveDataItemsReturnsTheMappedServerStatus() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        db.serverStatusDao.insert(TestDatabase.serverStatus(connectionId = 1, id = 1, serverName = "Active"))

        val items = dataSource.getLiveDataItems().observedValue()!!

        assertThat(items.map { it.serverName }).containsExactly("Active")
    }

    @Test
    fun getItemByIdReturnsTheServerStatusOrNull() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        db.serverStatusDao.insert(TestDatabase.serverStatus(connectionId = 1, id = 1, serverName = "Active"))

        assertThat(dataSource.getItemById(1)!!.serverName).isEqualTo("Active")
        assertThat(dataSource.getItemById(999)).isNull()
    }

    @Test
    fun getItemsReturnsAnEmptyList() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        dataSource.addItem(serverStatus(connectionId = 1, id = 1))

        assertThat(dataSource.getItems()).isEmpty()
    }

    @Test
    fun liveDataActiveItemReturnsTheActiveServerStatus() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        db.serverStatusDao.insert(TestDatabase.serverStatus(connectionId = 1, id = 1, serverName = "Active"))

        assertThat(dataSource.liveDataActiveItem.observedValue()!!.serverName).isEqualTo("Active")
    }

    /**
     * With no active connection `loadActiveServerStatus()` returns no row, so the `map` transform
     * hands a `null` entity to the data source. `ServerStatusDao.loadActiveServerStatus` used to be
     * declared non-null, which made the `?:` fallback dead code and turned this case - a fresh
     * install without any active connection - into a `NullPointerException` when the LiveData was
     * observed. Observing must instead yield the same default that the synchronous
     * [ServerStatusDataSource.activeItem] produces.
     */
    @Test
    fun liveDataActiveItemReturnsADefaultWhenNoConnectionIsActive() {
        val status = dataSource.liveDataActiveItem.observedValue()

        assertThat(status).isNotNull()
        assertThat(status!!.serverName).isEqualTo("Unknown")
        assertThat(status.serverVersion).isEqualTo("Unknown")
        assertThat(status.connectionId).isEqualTo(0)
        // The fallback does not insert a row when no connection is active.
        assertThat(db.serverStatusDao.serverStatusCount.observedValue()).isEqualTo(0)
    }
}
