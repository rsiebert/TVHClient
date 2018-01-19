package org.tvheadend.tvhclient.data.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.model.Channel;

import java.util.List;

@Dao
public interface ChannelDao {

    @Query("SELECT * FROM channels")
    LiveData<List<Channel>> loadAllChannels();

    @Query("SELECT * FROM channels WHERE id = :id")
    LiveData<Channel> loadChannel(int id);

    @Query("SELECT * FROM channels WHERE id = :id")
    Channel loadChannelSync(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Channel> channels);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Channel channel);

    @Update
    void update(Channel... channels);

    @Delete
    void delete(Channel channel);

    @Query("DELETE FROM channels")
    void deleteAll();
}
