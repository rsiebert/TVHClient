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

    @Query("SELECT * FROM TimerRecording")
    LiveData<List<TimerRecording>> getAll();

    @Query("SELECT * FROM TimerRecording WHERE id = :id")
    LiveData<TimerRecording> get(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TimerRecording recording);

    @Update
    void update(TimerRecording... recording);

    @Delete
    void delete(TimerRecording recording);
}
