package org.tvheadend.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.tvheadend.data.entity.ChannelTagEntity

@Dao
internal interface ChannelTagDao {

    @get:Query("SELECT COUNT (*) FROM channel_tags " +
            " WHERE $CONNECTION_IS_ACTIVE")
    val itemCountSync: Int

    @Query("SELECT DISTINCT * FROM channel_tags " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " ORDER BY tag_name")
    fun loadAllChannelTags(): LiveData<List<ChannelTagEntity>>

    @Query("SELECT DISTINCT * FROM channel_tags " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND channel_count > 0 " +
            " ORDER BY tag_name")
    fun loadOnlyNonEmptyChannelTagsSync(): List<ChannelTagEntity>

    @Query("SELECT DISTINCT * FROM channel_tags " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " ORDER BY tag_name")
    fun loadAllChannelTagsSync(): List<ChannelTagEntity>

    @Query("SELECT DISTINCT * FROM channel_tags " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND id = :id ")
    fun loadChannelTagByIdSync(id: Int): ChannelTagEntity

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(channelTag: ChannelTagEntity)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(items: List<ChannelTagEntity>)

    @Update
    fun update(channelTags: List<ChannelTagEntity>)

    @Update
    fun update(channelTags: ChannelTagEntity)

    @Delete
    fun delete(channelTag: ChannelTagEntity)

    @Query("DELETE FROM channel_tags")
    fun deleteAll()

    @Query("DELETE FROM channel_tags " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND id = :id ")
    fun deleteById(id: Int)

    @Query("SELECT id FROM channel_tags " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND is_selected = 1 " +
            " ORDER BY tag_name")
    fun loadAllSelectedItemIds(): LiveData<List<Int>?>

    companion object {

        const val CONNECTION_IS_ACTIVE = " connection_id IN (SELECT id FROM connections WHERE active = 1) "
    }
}
