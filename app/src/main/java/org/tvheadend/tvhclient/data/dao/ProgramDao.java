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

    String query = "SELECT p.*," +
            "c.channel_name AS channel_name, " +
            "c.channel_icon AS channel_icon, " +
            "recording.title AS recording_title, " +
            "recording.state AS recording_state, " +
            "recording.error AS recording_error " +
            "FROM programs AS p ";

    @Transaction
    @Query(query +
            "LEFT JOIN channels AS c ON c.id = p.channel_id " +
            "LEFT JOIN recordings AS recording ON recording.id = p.dvr_id " +
            "WHERE p.channel_id = :channelId AND ((p.start >= :time) OR (p.start <= :time AND p.stop >= :time)) " +
            "ORDER BY p.start ASC")
    LiveData<List<Program>> loadProgramsFromChannelWithinTime(int channelId, long time);

    @Transaction
    @Query(query +
            "LEFT JOIN channels AS c ON c.id = p.channel_id " +
            "LEFT JOIN recordings AS recording ON recording.id = p.dvr_id " +
            "WHERE p.id = :id")
    LiveData<Program> loadProgramById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Program> programs);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Program program);

    @Update
    void update(Program... program);

    @Delete
    void delete(Program program);

    @Query("DELETE FROM programs WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM programs")
    void deleteAll();

    @Query("SELECT * FROM programs " +
            "WHERE channel_id = :channelId " +
            "ORDER BY id DESC LIMIT 1")
    Program loadLastProgramFromChannelSync(int channelId);

    @Query("DELETE FROM programs WHERE channel_id = :channelId AND stop < :time")
    void deleteOldProgramsByChannel(int channelId, long time);
}
