package org.tvheadend.tvhclient.data.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelSubset;

import java.util.List;

@Dao
public interface ChannelDao {

    String base = "SELECT c.*, " +
            "program.id AS program_id, " +
            "program.title AS program_title, " +
            "program.subtitle AS program_subtitle, " +
            "program.start AS program_start, " +
            "program.stop AS program_stop, " +
            "program.content_type AS program_content_type, " +
            "next_program.id AS next_program_id, " +
            "next_program.title AS next_program_title " +
            "FROM channels AS c ";

    String orderBy = "ORDER BY CASE :sortOrder " +
            "   WHEN 0 THEN (c.display_number + 0) " +
            "   WHEN 1 THEN c.name " +
            "   WHEN 2 THEN (c.display_number + 0) " +
            "END";

    @Query("SELECT c.* FROM channels AS c " +
            "WHERE c.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND c.id = :id")
    Channel loadChannelByIdSync(int id);

    @Query("SELECT c.* FROM channels AS c " +
            "WHERE c.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            orderBy)
    List<Channel> loadAllChannelsSync(int sortOrder);

    @Transaction
    @Query("SELECT c.id, c.name, c.icon, c.number, c.number_minor " +
            "FROM channels AS c " +
            "WHERE c.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            "GROUP BY c.id " + orderBy)
    List<ChannelSubset> loadAllChannelsNamesOnlySync(int sortOrder);

    @Transaction
    @Query("SELECT c.id, c.name, c.icon, c.number, c.number_minor " +
            "FROM channels AS c " +
            "WHERE c.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND c.id IN (SELECT channel_id FROM tags_and_channels WHERE tag_id = :tagId) " +
            "GROUP BY c.id " + orderBy)
    List<ChannelSubset> loadAllChannelsNamesOnlyByTagSync(int tagId, int sortOrder);

    @Transaction
    @Query(base +
            "LEFT JOIN programs AS program ON program.start <= :time AND program.stop > :time AND program.channel_id = c.id " +
            "LEFT JOIN programs AS next_program ON next_program.start = program.stop AND next_program.channel_id = c.id " +
            "WHERE c.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            "GROUP BY c.id " + orderBy)
    List<Channel> loadAllChannelsByTimeSync(long time, int sortOrder);

    @Transaction
    @Query(base +
            "LEFT JOIN programs AS program ON program.start <= :time AND program.stop > :time AND program.channel_id = c.id " +
            "LEFT JOIN programs AS next_program ON next_program.start = program.stop AND next_program.channel_id = c.id " +
            "WHERE c.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND c.id IN (SELECT channel_id FROM tags_and_channels WHERE tag_id = :tagId) " +
            "GROUP BY c.id " + orderBy)
    List<Channel> loadAllChannelsByTimeAndTagSync(long time, int tagId, int sortOrder);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Channel channel);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<Channel> channels);

    @Update
    void update(Channel channel);

    @Delete
    void delete(Channel channel);

    @Query("DELETE FROM channels")
    void deleteAll();

    @Query("SELECT COUNT (*) FROM channels " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1)")
    LiveData<Integer> getChannelCount();
}
