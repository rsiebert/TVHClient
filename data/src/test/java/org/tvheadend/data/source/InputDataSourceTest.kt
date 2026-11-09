package org.tvheadend.data.source

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.Input
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [InputDataSource].
 *
 * This data source is a pure in-memory `MutableList` holder: it never touches the database and has
 * no coroutine scope. It is therefore tested for exactly what it does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InputDataSourceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppRoomDatabase
    private lateinit var dataSource: InputDataSource

    @Before
    fun setUp() {
        TestDatabase.plantNoopTimber()
        db = TestDatabase.create()
        dataSource = InputDataSource(db)
    }

    @After
    fun tearDown() {
        TestDatabase.teardown(db)
    }

    private fun input(uuid: String): Input {
        return Input(uuid = uuid, input = "Input $uuid")
    }

    @Test
    fun addItemAppendsToTheList() : Unit = runBlocking {
        dataSource.addItem(input("a"))

        assertThat(dataSource.getItems().map { it.uuid }).containsExactly("a")
    }

    @Test
    fun updateItemReplacesTheExistingEntry() : Unit = runBlocking {
        dataSource.addItem(input("a"))

        // Input has no identity field, so this matches on equality and replaces in place. It used
        // to append unconditionally, leaving the stale entry behind.
        dataSource.updateItem(input("a"))

        assertThat(dataSource.getItems()).hasSize(1)
    }

    @Test
    fun removeItemRemovesTheEntry() : Unit = runBlocking {
        val item = input("a")
        dataSource.addItem(item)

        dataSource.removeItem(item)

        assertThat(dataSource.getItems()).isEmpty()
    }

    @Test
    fun removeItemsClearsTheList() : Unit = runBlocking {
        dataSource.addItem(input("a"))
        dataSource.addItem(input("b"))

        dataSource.removeItems()

        assertThat(dataSource.getItems()).isEmpty()
    }

    @Test
    fun getItemByIdReturnsTheEntryAtThatIndex() : Unit = runBlocking {
        dataSource.addItem(input("a"))
        dataSource.addItem(input("b"))

        assertThat(dataSource.getItemById(0).uuid).isEqualTo("a")
        assertThat(dataSource.getItemById(1).uuid).isEqualTo("b")
    }

    @Test
    fun getLiveDataItemCountReflectsTheListSize() {
        dataSource.addItem(input("a"))
        dataSource.addItem(input("b"))

        assertThat(dataSource.getLiveDataItemCount().observedValue()).isEqualTo(2)
    }

    @Test
    fun getLiveDataItemsReturnsTheList() {
        dataSource.addItem(input("a"))

        assertThat(dataSource.getLiveDataItems().observedValue()!!.map { it.uuid }).containsExactly("a")
    }
}
