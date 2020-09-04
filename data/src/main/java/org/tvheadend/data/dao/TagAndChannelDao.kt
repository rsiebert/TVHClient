package org.tvheadend.data.dao

import androidx.room.*
import org.tvheadend.data.entity.TagAndChannelEntity

@Dao
internal abstract class TagAndChannelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(tagAndChannel: TagAndChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(tagAndChannel: List<TagAndChannelEntity>)

    @Update
    abstract fun update(tagAndChannel: TagAndChannelEntity)

    @Delete
    abstract fun delete(tagAndChannel: TagAndChannelEntity)

    @Delete
    abstract fun delete(tagAndChannel: List<TagAndChannelEntity>)

    @Transaction
    open fun insertAndDelete(newTagAndChannels: List<TagAndChannelEntity>, oldTagAndChannels: List<TagAndChannelEntity>) {
        delete(oldTagAndChannels)
        insert(newTagAndChannels)
    }

    @Query("DELETE FROM tags_and_channels " +
            " WHERE connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND tag_id = :id")
    abstract fun deleteByTagId(id: Int)

    @Query("DELETE FROM tags_and_channels")
    abstract fun deleteAll()
}
