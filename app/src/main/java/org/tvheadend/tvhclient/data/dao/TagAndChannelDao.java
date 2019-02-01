package org.tvheadend.tvhclient.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import org.tvheadend.tvhclient.data.entity.TagAndChannel;

import java.util.List;

@Dao
public abstract class TagAndChannelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(TagAndChannel tagAndChannel);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(List<TagAndChannel> tagAndChannel);

    @Update
    public abstract void update(TagAndChannel... tagAndChannel);

    @Delete
    public abstract void delete(TagAndChannel tagAndChannel);

    @Delete
    public abstract void delete(List<TagAndChannel> tagAndChannel);

    @Transaction
    public void insertAndDelete(List<TagAndChannel> newTagAndChannels, List<TagAndChannel> oldTagAndChannels) {
        delete(oldTagAndChannels);
        insert(newTagAndChannels);
    }

    @Query("DELETE FROM tags_and_channels " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1) "+
            " AND tag_id = :id")
    public abstract void deleteByTagId(int id);

    @Query("DELETE FROM tags_and_channels")
    public abstract void deleteAll();
}
