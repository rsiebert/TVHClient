package org.tvheadend.tvhclient.data.local.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.entity.TagAndChannel;

import java.util.ArrayList;

@Dao
public interface TagAndChannelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TagAndChannel tagAndChannel);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(ArrayList<TagAndChannel> tagAndChannels);

    @Update
    void update(TagAndChannel... tagAndChannel);

    @Delete
    void delete(TagAndChannel tagAndChannel);

    @Query("DELETE FROM tags_and_channels " +
            "WHERE tag_id = :id " +
            " AND connection_id IN (SELECT id FROM connections WHERE active = 1)")
    void deleteByTagId(int id);

    @Query("DELETE FROM tags_and_channels")
    void deleteAll();
}
