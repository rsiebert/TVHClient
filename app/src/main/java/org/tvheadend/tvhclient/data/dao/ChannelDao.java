package org.tvheadend.tvhclient.data.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.entity.Channel;

import java.util.List;

@Dao
public interface ChannelDao {

    @Query("SELECT c.*, " +
            "program.id AS program_id, " +
            "program.title AS program_title, " +
            "program.subtitle AS program_subtitle, " +
            "program.start AS program_start, " +
            "program.stop AS program_stop, " +
            "program.content_type AS program_content_type, " +
            "next_program.id AS next_program_id, " +
            "next_program.title AS next_program_title, " +
            "recording.id AS recording_id, " +
            "recording.title AS recording_title, " +
            "recording.state AS recording_state, " +
            "recording.error AS recording_error " +
            "FROM channels AS c " +
            "LEFT JOIN programs AS program ON program.start <= :time AND program.stop > :time AND program.channel_id = c.id " +
            "LEFT JOIN programs AS next_program ON next_program.id = program.id AND next_program.channel_id = c.id " +
            "LEFT JOIN recordings AS recording ON recording.id = program.dvr_id " +
            "ORDER BY c.channel_name ASC")
    List<Channel> loadAllChannelsByTimeSync(long time);

    @Query("SELECT * FROM channels")
    List<Channel> loadAllChannelsSync();

    @Query("SELECT * FROM channels")
    LiveData<List<Channel>> loadAllChannels();

    @Query("SELECT * FROM channels WHERE id = :id")
    Channel loadChannelByIdSync(int id);

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

    @Query("SELECT c.*, " +
            "program.id AS program_id, " +
            "program.title AS program_title, " +
            "program.subtitle AS program_subtitle, " +
            "program.start AS program_start, " +
            "program.stop AS program_stop, " +
            "program.content_type AS program_content_type, " +
            "next_program.id AS next_program_id, " +
            "next_program.title AS next_program_title, " +
            "recording.id AS recording_id, " +
            "recording.title AS recording_title, " +
            "recording.state AS recording_state, " +
            "recording.error AS recording_error " +
            "FROM channels AS c " +
            "LEFT JOIN programs AS program ON program.start <= :time AND program.stop > :time AND program.channel_id = c.id " +
            "LEFT JOIN programs AS next_program ON next_program.id = program.id AND next_program.channel_id = c.id " +
            "LEFT JOIN recordings AS recording ON recording.id = program.dvr_id " +
            "WHERE c.id IN (SELECT channel_id FROM tags_and_channels WHERE tag_id = :id) " +
            "ORDER BY c.channel_name ASC")
    List<Channel> loadAllChannelsByTimeAndTagSync(long time, int id);
}
