package org.tvheadend.tvhclient.data.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.entity.TimerRecording;

import java.util.List;

@Dao
public interface TimerRecordingDao {

    String RECORDING_BASE_QUERY = "SELECT DISTINCT rec.*, " +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM timer_recordings AS rec " +
            "LEFT JOIN channels AS c ON  c.id = rec.channel_id ";

    String CONNECTION_IS_ACTIVE = " rec.connection_id IN (SELECT id FROM connections WHERE active = 1) ";

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE)
    LiveData<List<TimerRecording>> loadAllRecordings();

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND rec.id = :id")
    LiveData<TimerRecording> loadRecordingById(String id);

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND rec.id = :id")
    TimerRecording loadRecordingByIdSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TimerRecording recording);

    @Update
    void update(TimerRecording recording);

    @Delete
    void delete(TimerRecording recording);

    @Query("DELETE FROM timer_recordings " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND id = :id")
    void deleteById(String id);

    @Query("DELETE FROM timer_recordings")
    void deleteAll();

    @Query("SELECT COUNT (*) FROM timer_recordings AS rec " +
            "WHERE " + CONNECTION_IS_ACTIVE)
    LiveData<Integer> getRecordingCount();
}
