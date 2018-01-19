package org.tvheadend.tvhclient.data.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.model.Recording;

import java.util.List;

@Dao
public interface RecordingDao {

    @Query("SELECT * FROM recordings WHERE error IS NULL AND state = 'completed'")
    LiveData<List<Recording>> loadAllCompletedRecordings();

    @Query("SELECT * FROM recordings WHERE error IS NULL AND (state = 'recording' OR state = 'scheduled')")
    LiveData<List<Recording>> loadAllScheduledRecordings();

    @Query("SELECT * FROM recordings WHERE " +
            "(error IS NOT NULL AND (state='missed'  OR state='invalid')) " +
            " OR (error IS NULL  AND state='missed') " +
            " OR (error='Aborted by user' AND state='completed')")
    LiveData<List<Recording>> loadAllFailedRecordings();

    @Query("SELECT * FROM recordings WHERE error = 'File missing' AND state = 'completed'")
    LiveData<List<Recording>> loadAllRemovedRecordings();

    @Query("SELECT * FROM recordings WHERE id = :id")
    LiveData<Recording> loadRecording(int id);

    @Query("SELECT * FROM recordings WHERE id = :id")
    Recording loadRecordingSync(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Recording> recordings);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Recording recording);

    @Update
    void update(Recording... recording);

    @Delete
    void delete(Recording recording);

    @Query("DELETE FROM recordings")
    void deleteAll();
}
