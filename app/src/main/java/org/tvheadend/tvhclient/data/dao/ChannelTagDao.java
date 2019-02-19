package org.tvheadend.tvhclient.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import org.tvheadend.tvhclient.domain.entity.ChannelTag;

import java.util.List;

@Dao
public interface ChannelTagDao {

    String CONNECTION_IS_ACTIVE = " connection_id IN (SELECT id FROM connections WHERE active = 1) ";

    @Query("SELECT DISTINCT * FROM channel_tags " +
            "WHERE " + CONNECTION_IS_ACTIVE +
            "ORDER BY tag_name")
    LiveData<List<ChannelTag>> loadAllChannelTags();

    @Query("SELECT DISTINCT * FROM channel_tags " +
            "WHERE " + CONNECTION_IS_ACTIVE +
            "ORDER BY tag_name")
    List<ChannelTag> loadAllChannelTagsSync();

    @Query("SELECT DISTINCT * FROM channel_tags " +
            "WHERE " + CONNECTION_IS_ACTIVE +
            "AND id = :id ")
    ChannelTag loadChannelTagByIdSync(int id);

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ChannelTag channelTag);

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<ChannelTag> items);

    @Update
    void update(List<ChannelTag> channelTags);

    @Update
    void update(ChannelTag channelTags);

    @Delete
    void delete(ChannelTag channelTag);

    @Query("DELETE FROM channel_tags")
    void deleteAll();

    @Query("DELETE FROM channel_tags " +
            "WHERE " + CONNECTION_IS_ACTIVE +
            "AND id = :id ")
    void deleteById(int id);

    @Query("SELECT id FROM channel_tags " +
            "WHERE " + CONNECTION_IS_ACTIVE +
            "AND is_selected = 1 " +
            "ORDER BY tag_name")
    LiveData<List<Integer>> loadAllSelectedItemIds();

    @Query("SELECT COUNT (*) FROM channel_tags " +
            "WHERE " + CONNECTION_IS_ACTIVE)
    int getItemCountSync();
}
