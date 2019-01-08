package org.tvheadend.tvhclient.data.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.RoomWarnings;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.EpgProgram;

import java.util.List;

@Dao
public interface ProgramDao {

    String PROGRAM_BASE_QUERY = "SELECT DISTINCT p.*," +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM programs AS p " +
            "LEFT JOIN channels AS c ON c.id = p.channel_id ";

    String EPG_PROGRAM_BASE_QUERY = "SELECT DISTINCT p.id, " +
            "p.title, p.subtitle, " +
            "p.channel_id, " +
            "p.connection_id, " +
            "p.start, p.stop, " +
            "p.content_type, " +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM programs AS p ";

    String CONNECTION_IS_ACTIVE = " p.connection_id IN (SELECT id FROM connections WHERE active = 1) ";

    @Transaction
    @Query(PROGRAM_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND ((p.start >= :time) " +
            "  OR (p.start <= :time AND p.stop >= :time)) " +
            "GROUP BY p.id " +
            "ORDER BY p.start, p.channel_name ASC")
    LiveData<List<Program>> loadProgramsFromTime(long time);

    @Transaction
    @Query(PROGRAM_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND p.channel_id = :channelId " +
            " AND ((p.start >= :time) " +
            "  OR (p.start <= :time AND p.stop >= :time)) " +
            "GROUP BY p.id " +
            "ORDER BY p.start ASC")
    LiveData<List<Program>> loadProgramsFromChannelFromTime(int channelId, long time);

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Transaction
    @Query(EPG_PROGRAM_BASE_QUERY +
            "LEFT JOIN channels AS c ON c.id = channel_id " +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND channel_id = :channelId " +
            // Program is within time slot
            " AND ((start >= :startTime AND stop <= :endTime) " +
            // Program is at the beginning of time slot
            "  OR (start <= :startTime AND stop > :startTime) " +
            // Program is at the end of the time slot
            "  OR (start < :endTime AND stop >= :endTime)) " +
            "GROUP BY p.id " +
            "ORDER BY start ASC")
    List<EpgProgram> loadProgramsFromChannelBetweenTimeSync(int channelId, long startTime, long endTime);

    @Transaction
    @Query(PROGRAM_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            "GROUP BY p.id " +
            "ORDER BY p.start, p.channel_name ASC")
    LiveData<List<Program>> loadPrograms();

    @Transaction
    @Query(PROGRAM_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            "GROUP BY p.id " +
            "ORDER BY p.start, p.channel_name ASC")
    List<Program> loadProgramsSync();

    @Transaction
    @Query(PROGRAM_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND p.id = :id")
    LiveData<Program> loadProgramById(int id);

    @Transaction
    @Query(PROGRAM_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND p.id = :id")
    Program loadProgramByIdSync(int id);

    @Query(PROGRAM_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
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

    @Query("SELECT COUNT (*) FROM programs AS p " +
            "WHERE " + CONNECTION_IS_ACTIVE)
    LiveData<Integer> getItemCount();

    @Query("SELECT COUNT (*) FROM programs AS p " +
            "WHERE " + CONNECTION_IS_ACTIVE)
    int getItemCountSync();
}
