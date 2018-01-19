package org.tvheadend.tvhclient.data.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.model.TimerRecording;

import java.util.List;

@Dao
public interface  TimerRecordingDao {

    @Query("SELECT * FROM timer_recordings")
    LiveData<List<TimerRecording>> loadAllRecordings();

    @Query("SELECT * FROM timer_recordings WHERE id = :id")
    LiveData<TimerRecording> loadRecording(String id);

    @Query("SELECT * FROM timer_recordings WHERE id = :id")
    TimerRecording loadRecordingSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TimerRecording> recordings);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TimerRecording recording);

    @Update
    void update(TimerRecording... recording);

    @Delete
    void delete(TimerRecording recording);

    @Query("DELETE FROM timer_recordings")
    void deleteAll();
}
