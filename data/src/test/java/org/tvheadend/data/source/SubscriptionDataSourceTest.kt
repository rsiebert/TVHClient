package org.tvheadend.data.source

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
import org.tvheadend.data.entity.Subscription
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [SubscriptionDataSource].
 *
 * This data source is a pure in-memory `MutableList` holder: it never touches the database and has
 * no coroutine scope. It is therefore tested for exactly what it does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SubscriptionDataSourceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppRoomDatabase
    private lateinit var dataSource: SubscriptionDataSource

    @Before
    fun setUp() {
        TestDatabase.plantNoopTimber()
        db = TestDatabase.create()
        dataSource = SubscriptionDataSource(db)
    }

    @After
    fun tearDown() {
        TestDatabase.teardown(db)
    }

    private fun subscription(id: Int): Subscription {
        return Subscription(id = id, title = "Subscription $id")
    }

    @Test
    fun addItemAppendsToTheList() {
        dataSource.addItem(subscription(1))

        assertThat(dataSource.getItems().map { it.id }).containsExactly(1)
    }

    @Test
    fun updateItemAppendsRatherThanReplacing() {
        dataSource.addItem(subscription(1))

        // The implementation simply adds, so updating an existing entry appends a second one.
        dataSource.updateItem(subscription(1))

        assertThat(dataSource.getItems()).hasSize(2)
    }

    @Test
    fun removeItemRemovesTheEntry() {
        val item = subscription(1)
        dataSource.addItem(item)

        dataSource.removeItem(item)

        assertThat(dataSource.getItems()).isEmpty()
    }

    @Test
    fun removeItemsClearsTheList() {
        dataSource.addItem(subscription(1))
        dataSource.addItem(subscription(2))

        dataSource.removeItems()

        assertThat(dataSource.getItems()).isEmpty()
    }

    @Test
    fun getItemByIdReturnsTheEntryAtThatIndex() {
        dataSource.addItem(subscription(1))
        dataSource.addItem(subscription(2))

        assertThat(dataSource.getItemById(0).id).isEqualTo(1)
        assertThat(dataSource.getItemById(1).id).isEqualTo(2)
    }

    @Test
    fun getLiveDataItemCountReflectsTheListSize() {
        dataSource.addItem(subscription(1))
        dataSource.addItem(subscription(2))

        assertThat(dataSource.getLiveDataItemCount().observedValue()).isEqualTo(2)
    }

    @Test
    fun getLiveDataItemsReturnsTheList() {
        dataSource.addItem(subscription(1))

        assertThat(dataSource.getLiveDataItems().observedValue()!!.map { it.id }).containsExactly(1)
    }

    @Test
    fun getLiveDataItemByIdReturnsTheEntry() {
        dataSource.addItem(subscription(1))

        assertThat(dataSource.getLiveDataItemById(0).observedValue()!!.id).isEqualTo(1)
    }
}
