package org.tvheadend.tvhclient.data.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.model.SeriesRecording;

import java.util.List;

@Dao
public interface SeriesRecordingDao {

    @Query("SELECT * FROM series_recordings")
    LiveData<List<SeriesRecording>> getAll();

    @Query("SELECT * FROM series_recordings WHERE id = :id")
    LiveData<SeriesRecording> get(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SeriesRecording recording);

    @Update
    void update(SeriesRecording... recording);

    @Delete
    void delete(SeriesRecording recording);
}
