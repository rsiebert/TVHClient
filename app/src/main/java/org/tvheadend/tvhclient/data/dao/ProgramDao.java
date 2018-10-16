package org.tvheadend.tvhclient.data.dao;

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

    String base = "SELECT DISTINCT p.*," +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM programs AS p " +
            "LEFT JOIN channels AS c ON c.id = p.channel_id ";

    @Transaction
    @Query(base +
            "WHERE p.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND ((p.start >= :time) " +
            "  OR (p.start <= :time AND p.stop >= :time)) " +
            "ORDER BY p.start, p.channel_name ASC")
    LiveData<List<Program>> loadProgramsFromTime(long time);

    @Transaction
    @Query(base +
            "WHERE p.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND p.channel_id = :channelId " +
            " AND ((p.start >= :time) " +
            "  OR (p.start <= :time AND p.stop >= :time)) " +
            "ORDER BY p.start ASC")
    LiveData<List<Program>> loadProgramsFromChannelFromTime(int channelId, long time);

    @Transaction
    @Query(base +
            "WHERE p.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND p.channel_id = :channelId " +
            // Program is within time slot
            " AND ((p.start >= :startTime AND p.stop <= :endTime) " +
            // Program is at the beginning of time slot
            "  OR (p.start <= :startTime AND p.stop > :startTime) " +
            // Program is at the end of the time slot
            "  OR (p.start < :endTime AND p.stop >= :endTime)) " +
            "ORDER BY p.start ASC")
    List<Program> loadProgramsFromChannelBetweenTimeSync(int channelId, long startTime, long endTime);

    @Transaction
    @Query(base +
            "WHERE p.connection_id IN (SELECT id FROM connections WHERE active = 1) "
            + "ORDER BY p.start, p.channel_name ASC")
    LiveData<List<Program>> loadPrograms();

    @Transaction
    @Query(base +
            "WHERE p.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND p.id = :id")
    LiveData<Program> loadProgramById(int id);

    @Transaction
    @Query(base +
            "WHERE p.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND p.id = :id")
    Program loadProgramByIdSync(int id);

    @Query(base +
            "WHERE p.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND p.channel_id = :channelId " +
            "ORDER BY start DESC LIMIT 1")
    Program loadLastProgramFromChannelSync(int channelId);

    @Query("DELETE FROM programs " +
            "WHERE stop < :time")
    void deleteProgramsByTime(long time);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<Program> programs);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Program program);

    @Update
    void update(List<Program> programs);

    @Update
    void update(Program... program);

    @Delete
    void delete(List<Program> programs);

    @Delete
    void delete(Program program);

    @Query("DELETE FROM programs " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND id = :id")
    void deleteById(int id);

    @Query("DELETE FROM programs")
    void deleteAll();

    @Query("SELECT COUNT (*) FROM programs " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1)")
    LiveData<Integer> getProgramCount();
}
