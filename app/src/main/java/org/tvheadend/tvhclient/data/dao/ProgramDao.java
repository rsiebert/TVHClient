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

    @Transaction
    @Query("SELECT * FROM programs " +
            "WHERE channel_id = :channelId AND ((start >= :time) OR (start <= :time AND stop >= :time)) " +
            "ORDER BY start ASC")
    LiveData<List<Program>> loadProgramsFromChannelWithinTime(int channelId, long time);

    @Transaction
    @Query("SELECT * FROM programs WHERE id = :id")
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
}
