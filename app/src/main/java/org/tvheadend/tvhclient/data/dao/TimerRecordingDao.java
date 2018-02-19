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
public interface  TimerRecordingDao {

    String query = "SELECT rec.*, " +
            "c.channel_name AS channel_name, " +
            "c.channel_icon AS channel_icon " +
            "FROM timer_recordings AS rec " +
            "LEFT JOIN channels AS c ON  c.id = rec.channel_id ";

    @Transaction
    @Query(query)
    LiveData<List<TimerRecording>> loadAllRecordings();

    @Transaction
    @Query(query + "WHERE rec.id = :id")
    LiveData<TimerRecording> loadRecordingById(String id);

    @Transaction
    @Query(query + "WHERE rec.id = :id")
    TimerRecording loadRecordingByIdSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TimerRecording> recordings);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TimerRecording recording);

    @Update
    void update(TimerRecording... recording);

    @Delete
    void delete(TimerRecording recording);

    @Query("DELETE FROM timer_recordings WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM timer_recordings")
    void deleteAll();
}
