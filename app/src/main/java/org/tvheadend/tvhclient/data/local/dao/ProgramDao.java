package org.tvheadend.tvhclient.data.local.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.entity.Program;

import java.util.List;

@Dao
public interface ProgramDao {

    @Transaction
    @Query("SELECT p.*," +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM programs AS p " +
            "LEFT JOIN channels AS c ON c.id = p.channel_id " +
            "WHERE p.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND p.channel_id = :channelId " +
            " AND ((p.start >= :time) " +
            "  OR (p.start <= :time AND p.stop >= :time)) " +
            "ORDER BY p.start ASC")
    LiveData<List<Program>> loadProgramsFromChannelWithinTime(int channelId, long time);

    @Transaction
    @Query("SELECT p.*," +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM programs AS p " +
            "LEFT JOIN channels AS c ON c.id = p.channel_id " +
            "WHERE p.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND p.id = :id")
    LiveData<Program> loadProgramById(int id);

    @Transaction
    @Query("SELECT p.*," +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM programs AS p " +
            "LEFT JOIN channels AS c ON c.id = p.channel_id " +
            "WHERE p.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND p.id = :id")
    Program loadProgramByIdSync(int id);

    @Query("SELECT p.*," +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM programs AS p " +
            "LEFT JOIN channels AS c ON c.id = p.channel_id " +
            "WHERE p.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND p.channel_id = :channelId " +
            "ORDER BY start DESC LIMIT 1")
    Program loadLastProgramFromChannelSync(int channelId);

    @Query("DELETE FROM programs " +
            "WHERE channel_id = :channelId AND stop < :time")
    void deleteOldProgramsByChannel(int channelId, long time);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Program> programs);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Program program);

    @Update
    void update(Program... program);

    @Update
    void update(List<Program> programs);

    @Delete
    void delete(Program program);

    @Query("DELETE FROM programs " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND id = :id")
    void deleteById(int id);

    @Query("DELETE FROM programs " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1)")
    void deleteAll();

    @Query("SELECT COUNT (*) FROM programs " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1)")
    LiveData<Integer> getProgramCount();
}
