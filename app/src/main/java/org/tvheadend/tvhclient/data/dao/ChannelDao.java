package org.tvheadend.tvhclient.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.EpgChannel;

import java.util.List;
import java.util.Set;

@Dao
public interface ChannelDao {

    String CHANNEL_BASE_QUERY = "SELECT DISTINCT c.*, " +
            "program.id AS program_id, " +
            "program.title AS program_title, " +
            "program.subtitle AS program_subtitle, " +
            "program.start AS program_start, " +
            "program.stop AS program_stop, " +
            "program.content_type AS program_content_type, " +
            "next_program.id AS next_program_id, " +
            "next_program.title AS next_program_title " +
            "FROM channels AS c ";

    String EPG_CHANNEL_BASE_QUERY = "SELECT c.id, " +
            "c.name, " +
            "c.icon, " +
            "c.number, " +
            "c.number_minor " +
            "FROM channels AS c ";

    String ORDER_BY = " ORDER BY " +
            "CASE :sortOrder WHEN 0 THEN c.server_order END ASC," +
            "CASE :sortOrder WHEN 1 THEN c.server_order END DESC," +
            "CASE :sortOrder WHEN 2 THEN c.id END ASC," +
            "CASE :sortOrder WHEN 3 THEN c.id END DESC," +
            "CASE :sortOrder WHEN 4 THEN c.name END ASC," +
            "CASE :sortOrder WHEN 5 THEN c.name END DESC," +
            "CASE :sortOrder WHEN 6 THEN (c.display_number + 0) END ASC," +
            "CASE :sortOrder WHEN 7 THEN (c.display_number + 0) END DESC";

    String CONNECTION_IS_ACTIVE = " c.connection_id IN (SELECT id FROM connections WHERE active = 1) ";

    @Query("SELECT c.* FROM channels AS c " +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND c.id = :id")
    Channel loadChannelByIdSync(int id);

    @Transaction
    @Query(CHANNEL_BASE_QUERY +
            "LEFT JOIN programs AS program ON program.start <= :time AND program.stop > :time AND program.channel_id = c.id " +
            "LEFT JOIN programs AS next_program ON next_program.start = program.stop AND next_program.channel_id = c.id " +
            "WHERE " + CONNECTION_IS_ACTIVE + " AND c.id = :id")
    Channel loadChannelByIdWithProgramsSync(int id, long time);

    @Query("SELECT c.* FROM channels AS c " +
            "WHERE " + CONNECTION_IS_ACTIVE +
            "GROUP BY c.id " +
            ORDER_BY)
    List<Channel> loadAllChannelsSync(int sortOrder);

    @Transaction
    @Query(CHANNEL_BASE_QUERY +
            "LEFT JOIN programs AS program ON program.start <= :time AND program.stop > :time AND program.channel_id = c.id " +
            "LEFT JOIN programs AS next_program ON next_program.start = program.stop AND next_program.channel_id = c.id " +
            "WHERE " + CONNECTION_IS_ACTIVE +
            "GROUP BY c.id " +
            ORDER_BY)
    LiveData<List<Channel>> loadAllChannelsByTime(long time, int sortOrder);

    @Transaction
    @Query(CHANNEL_BASE_QUERY +
            "LEFT JOIN programs AS program ON program.start <= :time AND program.stop > :time AND program.channel_id = c.id " +
            "LEFT JOIN programs AS next_program ON next_program.start = program.stop AND next_program.channel_id = c.id " +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND c.id IN (SELECT channel_id FROM tags_and_channels WHERE tag_id IN (:tagIds)) " +
            "GROUP BY c.id " +
            ORDER_BY)
    LiveData<List<Channel>> loadAllChannelsByTimeAndTag(long time, int sortOrder, List<Integer> tagIds);

    @Transaction
    @Query(CHANNEL_BASE_QUERY +
            "LEFT JOIN programs AS program ON program.start <= :time AND program.stop > :time AND program.channel_id = c.id " +
            "LEFT JOIN programs AS next_program ON next_program.start = program.stop AND next_program.channel_id = c.id " +
            "WHERE " + CONNECTION_IS_ACTIVE +
            "GROUP BY c.id " +
            ORDER_BY)
    List<Channel> loadAllChannelsByTimeSync(long time, int sortOrder);

    @Transaction
    @Query(CHANNEL_BASE_QUERY +
            "LEFT JOIN programs AS program ON program.start <= :time AND program.stop > :time AND program.channel_id = c.id " +
            "LEFT JOIN programs AS next_program ON next_program.start = program.stop AND next_program.channel_id = c.id " +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND c.id IN (SELECT channel_id FROM tags_and_channels WHERE tag_id IN (:tagIds)) " +
            "GROUP BY c.id " +
            ORDER_BY)
    List<Channel> loadAllChannelsByTimeAndTagSync(long time, Set<Integer> tagIds, int sortOrder);

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Channel channel);

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<Channel> channels);

    @Update
    void update(Channel channel);

    @Delete
    void delete(Channel channel);

    @Query("DELETE FROM channels")
    void deleteAll();

    @Query("SELECT COUNT (*) FROM channels AS c " +
            "WHERE " + CONNECTION_IS_ACTIVE)
    LiveData<Integer> getItemCount();

    @Query("SELECT COUNT (*) FROM channels AS c " +
            "WHERE " + CONNECTION_IS_ACTIVE)
    int getItemCountSync();

    @Transaction
    @Query(EPG_CHANNEL_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            ORDER_BY)
    LiveData<List<EpgChannel>> loadAllEpgChannels(int sortOrder);

    @Transaction
    @Query(EPG_CHANNEL_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND c.id IN (SELECT channel_id FROM tags_and_channels WHERE tag_id IN (:tagIds)) " +
            ORDER_BY)
    LiveData<List<EpgChannel>> loadAllEpgChannelsByTag(int sortOrder, List<Integer> tagIds);
}
