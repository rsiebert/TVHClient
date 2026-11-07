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
import org.tvheadend.data.entity.ChannelTag
import org.tvheadend.data.entity.ChannelTagEntity
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [ChannelTagDao] against a real in-memory database.
 *
 * The `channel_tags` table has a composite `(id, connection_id)` primary key. Every query filters
 * on the active connection, so most tests plant both an active and an inactive connection.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChannelTagDaoTest {

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
    fun countsOnlyTheChannelTagsOfTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.channelTagDao.insert(TestDatabase.channelTag(tagId = 1, connectionId = 1))
        db.channelTagDao.insert(TestDatabase.channelTag(tagId = 2, connectionId = 1))
        db.channelTagDao.insert(TestDatabase.channelTag(tagId = 3, connectionId = 2))

        assertThat(db.channelTagDao.itemCountSync).isEqualTo(2)
    }

    @Test
    fun loadAllChannelTagsOrdersByNameAndIgnoresTheInactiveConnection() {
        insertActiveAndInactiveConnections()
        db.channelTagDao.insert(TestDatabase.channelTag(tagId = 1, connectionId = 1, tagName = "News"))
        db.channelTagDao.insert(TestDatabase.channelTag(tagId = 2, connectionId = 1, tagName = "Movies"))
        db.channelTagDao.insert(TestDatabase.channelTag(tagId = 3, connectionId = 2, tagName = "Hidden"))

        val liveDataIds = db.channelTagDao.loadAllChannelTags().observedValue()!!.map { it.tagId }
        val syncIds = db.channelTagDao.loadAllChannelTagsSync().map { it.tagId }

        assertThat(liveDataIds).isEqualTo(listOf(2, 1))
        assertThat(syncIds).isEqualTo(listOf(2, 1))
    }

    @Test
    fun loadOnlyNonEmptyChannelTagsSyncFiltersOnChannelCount() {
        insertActiveAndInactiveConnections()
        db.channelTagDao.insert(TestDatabase.channelTag(tagId = 1, connectionId = 1, tagName = "Empty", channelCount = 0))
        db.channelTagDao.insert(TestDatabase.channelTag(tagId = 2, connectionId = 1, tagName = "Full", channelCount = 3))

        val ids = db.channelTagDao.loadOnlyNonEmptyChannelTagsSync().map { it.tagId }

        assertThat(ids).containsExactly(2)
    }

    @Test
    fun loadChannelTagByIdOnlyReturnsTagsOfTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.channelTagDao.insert(TestDatabase.channelTag(tagId = 1, connectionId = 1, tagName = "Active"))
        db.channelTagDao.insert(TestDatabase.channelTag(tagId = 1, connectionId = 2, tagName = "Inactive"))

        assertThat(db.channelTagDao.loadChannelTagByIdSync(1)!!.tagName).isEqualTo("Active")
        assertThat(db.channelTagDao.loadChannelTagByIdSync(999)).isNull()
    }

    @Test
    fun loadAllSelectedItemIdsReturnsTheSelectedTagsOrderedByName() {
        insertActiveAndInactiveConnections()
        db.channelTagDao.insert(TestDatabase.channelTag(tagId = 1, connectionId = 1, tagName = "Bravo", isSelected = true))
        db.channelTagDao.insert(TestDatabase.channelTag(tagId = 2, connectionId = 1, tagName = "Alpha", isSelected = true))
        db.channelTagDao.insert(TestDatabase.channelTag(tagId = 3, connectionId = 1, tagName = "Charlie", isSelected = false))
        db.channelTagDao.insert(TestDatabase.channelTag(tagId = 4, connectionId = 2, tagName = "Delta", isSelected = true))

        assertThat(db.channelTagDao.loadAllSelectedItemIds().observedValue()).isEqualTo(listOf(2, 1))
    }

    @Test
    fun insertsUpdatesAndDeletesChannelTags() {
        insertActiveAndInactiveConnections()
        db.channelTagDao.insert(listOf(
                TestDatabase.channelTag(tagId = 1, connectionId = 1, tagName = "One"),
                TestDatabase.channelTag(tagId = 2, connectionId = 1, tagName = "Two")))
        assertThat(db.channelTagDao.itemCountSync).isEqualTo(2)

        db.channelTagDao.update(TestDatabase.channelTag(tagId = 1, connectionId = 1, tagName = "Uno"))
        assertThat(db.channelTagDao.loadChannelTagByIdSync(1)!!.tagName).isEqualTo("Uno")

        db.channelTagDao.update(listOf(TestDatabase.channelTag(tagId = 2, connectionId = 1, tagName = "Zwei")))
        assertThat(db.channelTagDao.loadChannelTagByIdSync(2)!!.tagName).isEqualTo("Zwei")

        db.channelTagDao.delete(TestDatabase.channelTag(tagId = 1, connectionId = 1))
        assertThat(db.channelTagDao.loadChannelTagByIdSync(1)).isNull()

        db.channelTagDao.deleteById(2)
        assertThat(db.channelTagDao.itemCountSync).isEqualTo(0)

        db.channelTagDao.insert(TestDatabase.channelTag(tagId = 3, connectionId = 1))
        db.channelTagDao.deleteAll()
        assertThat(db.channelTagDao.itemCountSync).isEqualTo(0)
    }

    @Test
    fun mapsChannelTagToEntityAndBackIncludingTheIgnoredMembers() {
        val channelTag = ChannelTag(
                tagId = 4,
                tagName = "News",
                tagIndex = 2,
                tagIcon = "http://example.org/icon.png",
                tagTitledIcon = 1,
                channelCount = 3,
                connectionId = 1,
                isSelected = true,
                members = listOf(10, 11))

        val entity = ChannelTagEntity.from(channelTag)
        val restored = entity.toChannelTag()

        assertThat(restored).isEqualTo(channelTag)
        // `members` is @Ignore'd by Room but must survive the pure mapping.
        assertThat(restored.members).containsExactly(10, 11).inOrder()
    }
}
