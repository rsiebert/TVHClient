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
import org.tvheadend.data.entity.TagAndChannel
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [TagAndChannelDataSource] against a real in-memory database.
 *
 * The data source has no read accessor of its own, so the join-table row count is verified with a
 * raw query. [Dispatchers.Unconfined] is injected as the `ioScope` so every write runs inline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TagAndChannelDataSourceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppRoomDatabase
    private lateinit var dataSource: TagAndChannelDataSource

    @Before
    fun setUp() {
        TestDatabase.plantNoopTimber()
        db = TestDatabase.create()
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        dataSource = TagAndChannelDataSource(db, CoroutineScope(Dispatchers.Unconfined))
    }

    @After
    fun tearDown() {
        TestDatabase.teardown(db)
    }

    private fun tagAndChannel(tagId: Int, channelId: Int): TagAndChannel {
        return TagAndChannel(tagId = tagId, channelId = channelId, connectionId = 1)
    }

    private fun rowCount(): Int {
        db.query("SELECT COUNT(*) FROM tags_and_channels", null).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }

    @Test
    fun addItemStoresTheAssociation() {
        dataSource.addItem(tagAndChannel(tagId = 1, channelId = 10))

        assertThat(rowCount()).isEqualTo(1)
    }

    @Test
    fun updateItemKeepsTheAssociation() {
        dataSource.addItem(tagAndChannel(tagId = 1, channelId = 10))

        dataSource.updateItem(tagAndChannel(tagId = 1, channelId = 10))

        assertThat(rowCount()).isEqualTo(1)
    }

    @Test
    fun removeItemDeletesTheAssociation() {
        dataSource.addItem(tagAndChannel(tagId = 1, channelId = 10))

        dataSource.removeItem(tagAndChannel(tagId = 1, channelId = 10))

        assertThat(rowCount()).isEqualTo(0)
    }

    @Test
    fun addAndRemoveItemsReplacesTheOldAssociations() {
        dataSource.addItem(tagAndChannel(tagId = 1, channelId = 10))

        dataSource.addAndRemoveItems(
                newItems = listOf(tagAndChannel(tagId = 1, channelId = 11)),
                oldItems = listOf(tagAndChannel(tagId = 1, channelId = 10)))

        assertThat(rowCount()).isEqualTo(1)
    }

    @Test
    fun removeItemByTagIdDeletesTheTagAssociations() {
        dataSource.addItem(tagAndChannel(tagId = 1, channelId = 10))
        dataSource.addItem(tagAndChannel(tagId = 1, channelId = 11))
        dataSource.addItem(tagAndChannel(tagId = 2, channelId = 10))

        dataSource.removeItemByTagId(1)

        assertThat(rowCount()).isEqualTo(1)
    }

    @Test
    fun notImplementedGettersReturnEmpty() {
        dataSource.addItem(tagAndChannel(tagId = 1, channelId = 10))

        assertThat(dataSource.getLiveDataItemCount().observedValue()).isNull()
        assertThat(dataSource.getLiveDataItems().observedValue()).isNull()
        assertThat(dataSource.getItemById(1)).isNull()
        assertThat(dataSource.getItems()).isEmpty()
    }
}
