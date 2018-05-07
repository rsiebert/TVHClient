package org.tvheadend.tvhclient.data.dao;

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

    @Query("SELECT * FROM channel_tags")
    List<ChannelTag> loadAllChannelTagsSync();

    @Query("SELECT * FROM channel_tags WHERE id = :id")
    ChannelTag loadChannelTagByIdSync(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ChannelTag> channelTags);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ChannelTag channelTag);

    @Update
    void update(ChannelTag... channelTags);

    @Delete
    void delete(ChannelTag channelTag);

    @Query("DELETE FROM channel_tags " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1)")
    void deleteAll();

    @Query("DELETE FROM channel_tags " +
            "WHERE id = :id AND connection_id IN (SELECT id FROM connections WHERE active = 1)")
    void deleteById(int id);
}
