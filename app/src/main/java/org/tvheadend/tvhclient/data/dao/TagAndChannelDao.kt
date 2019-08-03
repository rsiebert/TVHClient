package org.tvheadend.tvhclient.data.dao

import androidx.room.*
import org.tvheadend.tvhclient.domain.entity.TagAndChannel

@Dao
abstract class TagAndChannelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(tagAndChannel: TagAndChannel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(tagAndChannel: List<TagAndChannel>)

    @Update
    abstract fun update(vararg tagAndChannel: TagAndChannel)

    @Delete
    abstract fun delete(tagAndChannel: TagAndChannel)

    @Delete
    abstract fun delete(tagAndChannel: List<TagAndChannel>)

    @Transaction
    open fun insertAndDelete(newTagAndChannels: List<TagAndChannel>, oldTagAndChannels: List<TagAndChannel>) {
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
