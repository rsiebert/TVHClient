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
import org.tvheadend.data.entity.Connection
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [ConnectionDataSource] against a real in-memory database.
 *
 * [Dispatchers.Unconfined] is injected as the `ioScope` so every write runs inline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConnectionDataSourceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppRoomDatabase
    private lateinit var dataSource: ConnectionDataSource

    @Before
    fun setUp() {
        TestDatabase.plantNoopTimber()
        db = TestDatabase.create()
        dataSource = ConnectionDataSource(db, CoroutineScope(Dispatchers.Unconfined))
    }

    @After
    fun tearDown() {
        TestDatabase.teardown(db)
    }

    private fun connection(id: Int = 0, name: String = "Connection", isActive: Boolean = false): Connection {
        return Connection(id = id, name = name, isActive = isActive)
    }

    @Test
    fun addItemInsertsTheConnectionAndItsServerStatus() {
        dataSource.addItem(connection(name = "Home"))

        val stored = db.connectionDao.loadAllConnectionsSync()
        assertThat(stored).hasSize(1)
        val id = stored[0].id
        assertThat(db.serverStatusDao.loadServerStatusByIdSync(id)).isNotNull()
    }

    @Test
    fun addItemWithAnActiveConnectionDisablesThePreviousActiveOne() {
        dataSource.addItem(connection(id = 1, name = "Old", isActive = true))
        dataSource.addItem(connection(id = 2, name = "New", isActive = true))

        assertThat(db.connectionDao.loadConnectionByIdSync(1)!!.isActive).isFalse()
        assertThat(db.connectionDao.loadConnectionByIdSync(2)!!.isActive).isTrue()
    }

    @Test
    fun updateItemReplacesTheStoredConnection() {
        dataSource.addItem(connection(id = 1, name = "Before"))

        dataSource.updateItem(connection(id = 1, name = "After"))

        assertThat(db.connectionDao.loadConnectionByIdSync(1)!!.name).isEqualTo("After")
    }

    @Test
    fun updateItemWithAnActiveConnectionDisablesThePreviousActiveOne() {
        dataSource.addItem(connection(id = 1, name = "Old", isActive = true))
        dataSource.addItem(connection(id = 2, name = "New", isActive = false))

        dataSource.updateItem(connection(id = 2, name = "New", isActive = true))

        assertThat(db.connectionDao.loadConnectionByIdSync(1)!!.isActive).isFalse()
        assertThat(db.connectionDao.loadConnectionByIdSync(2)!!.isActive).isTrue()
    }

    @Test
    fun removeItemDeletesTheConnectionAndItsServerStatus() {
        dataSource.addItem(connection(id = 1, name = "Home", isActive = true))
        assertThat(db.serverStatusDao.loadServerStatusByIdSync(1)).isNotNull()

        dataSource.removeItem(connection(id = 1))

        assertThat(db.connectionDao.loadConnectionByIdSync(1)).isNull()
        assertThat(db.serverStatusDao.loadServerStatusByIdSync(1)).isNull()
    }

    @Test
    fun getLiveDataItemCountReflectsTheStoredConnections() {
        dataSource.addItem(connection(id = 1, name = "One"))
        dataSource.addItem(connection(id = 2, name = "Two"))

        assertThat(dataSource.getLiveDataItemCount().observedValue()).isEqualTo(2)
    }

    @Test
    fun getLiveDataItemsReturnsTheMappedConnections() {
        dataSource.addItem(connection(id = 1, name = "One"))
        dataSource.addItem(connection(id = 2, name = "Two"))

        val items = dataSource.getLiveDataItems().observedValue()!!

        assertThat(items.map { it.id }).containsExactly(1, 2)
    }

    @Test
    fun getLiveDataItemByIdReturnsTheConnection() {
        dataSource.addItem(connection(id = 1, name = "One"))

        assertThat(dataSource.getLiveDataItemById(1).observedValue()!!.name).isEqualTo("One")
    }

    @Test
    fun getItemByIdReturnsTheConnectionOrNull() {
        dataSource.addItem(connection(id = 1, name = "One"))

        assertThat(dataSource.getItemById(1)!!.name).isEqualTo("One")
        assertThat(dataSource.getItemById(999)).isNull()
    }

    @Test
    fun getItemsReturnsEveryConnection() {
        dataSource.addItem(connection(id = 1, name = "One"))
        dataSource.addItem(connection(id = 2, name = "Two"))

        assertThat(dataSource.getItems().map { it.id }).containsExactly(1, 2)
    }

    @Test
    fun activeItemReturnsTheActiveConnection() {
        dataSource.addItem(connection(id = 1, name = "Active", isActive = true))
        dataSource.addItem(connection(id = 2, name = "Inactive", isActive = false))

        assertThat(dataSource.activeItem.id).isEqualTo(1)
        assertThat(dataSource.activeItem.name).isEqualTo("Active")
    }

    @Test
    fun activeItemReturnsADefaultWithMinusOneWhenNoneIsActive() {
        dataSource.addItem(connection(id = 1, name = "Inactive", isActive = false))

        assertThat(dataSource.activeItem.id).isEqualTo(-1)
    }

    @Test
    fun liveDataActiveItemReturnsTheActiveConnection() {
        dataSource.addItem(connection(id = 1, name = "Active", isActive = true))

        assertThat(dataSource.liveDataActiveItem.observedValue()!!.id).isEqualTo(1)
    }

    /**
     * With no active connection `loadActiveConnection()` returns no row, so the `map` transform
     * hands a `null` entity to the data source. `ConnectionDao.loadActiveConnection` used to be
     * declared non-null, which made the `?:` fallback dead code and turned this case - a fresh
     * install without any active connection - into a `NullPointerException` when the LiveData was
     * observed. Observing must instead yield the default `Connection()`, the same default the
     * synchronous [ConnectionDataSource.activeItem] produces when nothing is active.
     */
    @Test
    fun liveDataActiveItemReturnsADefaultWhenNoConnectionIsActive() {
        // Nothing is active: only an inactive connection exists.
        dataSource.addItem(connection(id = 1, name = "Inactive", isActive = false))

        val activeItem = dataSource.liveDataActiveItem.observedValue()

        assertThat(activeItem).isNotNull()
        assertThat(activeItem!!.id).isEqualTo(Connection().id)
        assertThat(activeItem.isActive).isFalse()
        assertThat(activeItem).isEqualTo(Connection())
    }

    @Test
    fun setSyncRequiredForActiveConnectionResetsTheSyncFlags() {
        dataSource.addItem(connection(id = 1, name = "Home", isActive = true))
        db.connectionDao.update(db.connectionDao.loadConnectionByIdSync(1)!!.copy(isSyncRequired = false, lastUpdate = 500L))

        dataSource.setSyncRequiredForActiveConnection()

        val stored = db.connectionDao.loadConnectionByIdSync(1)!!
        assertThat(stored.isSyncRequired).isTrue()
        assertThat(stored.lastUpdate).isEqualTo(0L)
    }

    @Test
    fun switchActiveConnectionMovesTheActiveFlag() {
        dataSource.addItem(connection(id = 1, name = "Old", isActive = true))
        dataSource.addItem(connection(id = 2, name = "New", isActive = false))

        dataSource.switchActiveConnection(oldId = 1, newId = 2)

        assertThat(db.connectionDao.loadConnectionByIdSync(1)!!.isActive).isFalse()
        assertThat(db.connectionDao.loadConnectionByIdSync(2)!!.isActive).isTrue()
    }
}
