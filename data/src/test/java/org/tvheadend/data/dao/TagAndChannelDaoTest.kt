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
import org.tvheadend.data.entity.TagAndChannel
import org.tvheadend.data.entity.TagAndChannelEntity
import org.tvheadend.data.testutil.TestDatabase

/**
 * Robolectric test for [TagAndChannelDao] against a real in-memory database.
 *
 * The DAO exposes no SELECT query, it is a pure write/join table keyed by
 * `(tag_id, channel_id, connection_id)`. The row count is therefore verified with a raw query
 * against the `tags_and_channels` table.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TagAndChannelDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppRoomDatabase

    @Before
    fun setUp() {
        TestDatabase.plantNoopTimber()
        db = TestDatabase.create()
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        db.connectionDao.insert(TestDatabase.inactiveConnection(id = 2))
    }

    @After
    fun tearDown() {
        TestDatabase.teardown(db)
    }

    private fun rowCount(): Int {
        db.query("SELECT COUNT(*) FROM tags_and_channels", null).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }

    @Test
    fun insertSingleAndListIncreasesTheRowCount() {
        db.tagAndChannelDao.insert(TestDatabase.tagAndChannel(tagId = 1, channelId = 10, connectionId = 1))
        db.tagAndChannelDao.insert(listOf(
                TestDatabase.tagAndChannel(tagId = 1, channelId = 11, connectionId = 1),
                TestDatabase.tagAndChannel(tagId = 2, channelId = 10, connectionId = 1)))

        assertThat(rowCount()).isEqualTo(3)
    }

    @Test
    fun insertReplacesAnExistingAssociation() {
        db.tagAndChannelDao.insert(TestDatabase.tagAndChannel(tagId = 1, channelId = 10, connectionId = 1))
        db.tagAndChannelDao.insert(TestDatabase.tagAndChannel(tagId = 1, channelId = 10, connectionId = 1))

        assertThat(rowCount()).isEqualTo(1)
    }

    @Test
    fun deleteRemovesSingleAndMultipleAssociations() {
        db.tagAndChannelDao.insert(listOf(
                TestDatabase.tagAndChannel(tagId = 1, channelId = 10, connectionId = 1),
                TestDatabase.tagAndChannel(tagId = 1, channelId = 11, connectionId = 1),
                TestDatabase.tagAndChannel(tagId = 1, channelId = 12, connectionId = 1)))

        db.tagAndChannelDao.delete(TestDatabase.tagAndChannel(tagId = 1, channelId = 10, connectionId = 1))
        assertThat(rowCount()).isEqualTo(2)

        db.tagAndChannelDao.delete(listOf(TestDatabase.tagAndChannel(tagId = 1, channelId = 11, connectionId = 1)))
        assertThat(rowCount()).isEqualTo(1)
    }

    @Test
    fun insertAndDeleteReplacesTheOldAssociationsWithTheNewOnes() {
        db.tagAndChannelDao.insert(listOf(
                TestDatabase.tagAndChannel(tagId = 1, channelId = 10, connectionId = 1),
                TestDatabase.tagAndChannel(tagId = 1, channelId = 11, connectionId = 1)))

        db.tagAndChannelDao.insertAndDelete(
                newTagAndChannels = listOf(TestDatabase.tagAndChannel(tagId = 1, channelId = 12, connectionId = 1)),
                oldTagAndChannels = listOf(
                        TestDatabase.tagAndChannel(tagId = 1, channelId = 10, connectionId = 1),
                        TestDatabase.tagAndChannel(tagId = 1, channelId = 11, connectionId = 1)))

        assertThat(rowCount()).isEqualTo(1)
    }

    @Test
    fun deleteByTagIdOnlyRemovesAssociationsOfTheActiveConnection() {
        db.tagAndChannelDao.insert(listOf(
                TestDatabase.tagAndChannel(tagId = 1, channelId = 10, connectionId = 1),
                TestDatabase.tagAndChannel(tagId = 1, channelId = 11, connectionId = 1),
                TestDatabase.tagAndChannel(tagId = 2, channelId = 10, connectionId = 1),
                // Belongs to the inactive connection and must not be deleted.
                TestDatabase.tagAndChannel(tagId = 1, channelId = 10, connectionId = 2)))

        db.tagAndChannelDao.deleteByTagId(1)

        assertThat(rowCount()).isEqualTo(2)
    }

    @Test
    fun deleteAllRemovesEveryAssociation() {
        db.tagAndChannelDao.insert(listOf(
                TestDatabase.tagAndChannel(tagId = 1, channelId = 10, connectionId = 1),
                TestDatabase.tagAndChannel(tagId = 1, channelId = 10, connectionId = 2)))

        db.tagAndChannelDao.deleteAll()

        assertThat(rowCount()).isEqualTo(0)
    }

    @Test
    fun mapsTagAndChannelToEntityAndBack() {
        val tagAndChannel = TagAndChannel(tagId = 3, channelId = 7, connectionId = 1)

        assertThat(TagAndChannelEntity.from(tagAndChannel).toTagAndChannel()).isEqualTo(tagAndChannel)
    }
}
