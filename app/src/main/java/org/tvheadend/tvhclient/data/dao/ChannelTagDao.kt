package org.tvheadend.tvhclient.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.tvheadend.tvhclient.domain.entity.ChannelTag

@Dao
interface ChannelTagDao {

    @get:Query("SELECT COUNT (*) FROM channel_tags " +
            " WHERE $CONNECTION_IS_ACTIVE")
    val itemCountSync: Int

    @Query("SELECT DISTINCT * FROM channel_tags " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " ORDER BY tag_name")
    fun loadAllChannelTags(): LiveData<List<ChannelTag>>

    @Query("SELECT DISTINCT * FROM channel_tags " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " ORDER BY tag_name")
    fun loadAllChannelTagsSync(): List<ChannelTag>

    @Query("SELECT DISTINCT * FROM channel_tags " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND id = :id ")
    fun loadChannelTagByIdSync(id: Int): ChannelTag

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(channelTag: ChannelTag)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(items: List<ChannelTag>)

    @Update
    fun update(channelTags: List<ChannelTag>)

    @Update
    fun update(channelTags: ChannelTag)

    @Delete
    fun delete(channelTag: ChannelTag)

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
