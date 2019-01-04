package org.tvheadend.tvhclient.data.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.entity.ChannelTag;

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ChannelTag channelTag);

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
    List<Integer> loadAllSelectedChannelTagIds();
}
