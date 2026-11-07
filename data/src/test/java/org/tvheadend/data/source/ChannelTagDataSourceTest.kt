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
import org.tvheadend.data.entity.ChannelTag
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [ChannelTagDataSource] against a real in-memory database.
 *
 * [Dispatchers.Unconfined] is injected as the `ioScope` so every write runs inline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChannelTagDataSourceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppRoomDatabase
    private lateinit var dataSource: ChannelTagDataSource

    @Before
    fun setUp() {
        TestDatabase.plantNoopTimber()
        db = TestDatabase.create()
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        dataSource = ChannelTagDataSource(db, CoroutineScope(Dispatchers.Unconfined))
    }

    @After
    fun tearDown() {
        TestDatabase.teardown(db)
    }

    private fun channelTag(tagId: Int, name: String = "Tag $tagId", channelCount: Int = 0, isSelected: Boolean = false): ChannelTag {
        return ChannelTag(tagId = tagId, tagName = name, channelCount = channelCount, isSelected = isSelected, connectionId = 1)
    }

    @Test
    fun itemCountAndAddItem() {
        assertThat(dataSource.itemCount).isEqualTo(0)

        dataSource.addItem(channelTag(tagId = 1))

        assertThat(dataSource.itemCount).isEqualTo(1)
        assertThat(db.channelTagDao.loadChannelTagByIdSync(1)!!.tagName).isEqualTo("Tag 1")
    }

    @Test
    fun addItemsStoresAllTags() {
        dataSource.addItems(listOf(channelTag(tagId = 1), channelTag(tagId = 2)))

        assertThat(dataSource.itemCount).isEqualTo(2)
    }

    @Test
    fun updateItemReplacesTheStoredTag() {
        dataSource.addItem(channelTag(tagId = 1, name = "Before"))

        dataSource.updateItem(channelTag(tagId = 1, name = "After"))

        assertThat(db.channelTagDao.loadChannelTagByIdSync(1)!!.tagName).isEqualTo("After")
    }

    @Test
    fun removeItemDeletesTheTag() {
        dataSource.addItem(channelTag(tagId = 1))

        dataSource.removeItem(channelTag(tagId = 1))

        assertThat(dataSource.itemCount).isEqualTo(0)
    }

    @Test
    fun updateSelectedChannelTagsSetsAndClearsTheSelection() {
        dataSource.addItem(channelTag(tagId = 1, name = "One"))
        dataSource.addItem(channelTag(tagId = 2, name = "Two"))

        dataSource.updateSelectedChannelTags(setOf(2))

        assertThat(db.channelTagDao.loadChannelTagByIdSync(2)!!.isSelected).isTrue()
        assertThat(db.channelTagDao.loadChannelTagByIdSync(1)!!.isSelected).isFalse()
    }

    @Test
    fun getLiveDataItemsReturnsTheMappedTags() {
        dataSource.addItem(channelTag(tagId = 1, name = "Bravo"))
        dataSource.addItem(channelTag(tagId = 2, name = "Alpha"))

        val items = dataSource.getLiveDataItems().observedValue()!!

        assertThat(items.map { it.tagId }).isEqualTo(listOf(2, 1))
    }

    @Test
    fun getItemByIdReturnsTheTagOrNull() {
        dataSource.addItem(channelTag(tagId = 1, name = "One"))

        assertThat(dataSource.getItemById(1)!!.tagName).isEqualTo("One")
        assertThat(dataSource.getItemById(999)).isNull()
    }

    @Test
    fun getItemsReturnsAllTags() {
        dataSource.addItem(channelTag(tagId = 1))
        dataSource.addItem(channelTag(tagId = 2))

        assertThat(dataSource.getItems().map { it.tagId }).containsExactly(1, 2)
    }

    @Test
    fun getNonEmptyItemsFiltersByChannelCountWhenLoadAllIsFalse() {
        dataSource.addItem(channelTag(tagId = 1, name = "Empty", channelCount = 0))
        dataSource.addItem(channelTag(tagId = 2, name = "Full", channelCount = 3))

        assertThat(dataSource.getNonEmptyItems(loadAll = true).map { it.tagId }).containsExactly(1, 2)
        assertThat(dataSource.getNonEmptyItems(loadAll = false).map { it.tagId }).containsExactly(2)
    }

    @Test
    fun liveDataSelectedItemIdsReflectsTheStoredSelection() {
        dataSource.addItem(channelTag(tagId = 1, name = "One", isSelected = true))
        dataSource.addItem(channelTag(tagId = 2, name = "Two", isSelected = false))

        assertThat(dataSource.liveDataSelectedItemIds.observedValue()).isEqualTo(listOf(1))
    }

    @Test
    fun notImplementedLiveDataGettersReturnEmpty() {
        dataSource.addItem(channelTag(tagId = 1))

        assertThat(dataSource.getLiveDataItemCount().observedValue()).isNull()
        assertThat(dataSource.getLiveDataItemById(1).observedValue()).isNull()
    }
}
