package org.tvheadend.tvhclient.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import org.tvheadend.tvhclient.domain.entity.TimerRecording;

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
