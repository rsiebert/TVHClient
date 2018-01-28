package org.tvheadend.tvhclient.data.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.entity.Recording;

import java.util.List;

@Dao
public interface RecordingDao {

    String query = "SELECT rec.*, " +
            "c.channel_name AS channel_name, " +
            "c.channel_icon AS channel_icon " +
            "FROM recordings AS rec " +
            "LEFT JOIN channels AS c ON  c.id = rec.channel_id ";

    @Query(query + "WHERE rec.error IS NULL AND rec.state = 'completed'")
    LiveData<List<Recording>> loadAllCompletedRecordings();

    @Query(query + "WHERE rec.error IS NULL AND (rec.state = 'recording' OR rec.state = 'scheduled')")
    LiveData<List<Recording>> loadAllScheduledRecordings();

    @Query(query + "WHERE " +
            "(rec.error IS NOT NULL AND (rec.state='missed'  OR rec.state='invalid')) " +
            " OR (rec.error IS NULL  AND rec.state='missed') " +
            " OR (rec.error='Aborted by user' AND rec.state='completed')")
    LiveData<List<Recording>> loadAllFailedRecordings();

    @Query(query + "WHERE rec.error = 'File missing' AND rec.state = 'completed'")
    LiveData<List<Recording>> loadAllRemovedRecordings();

    @Query(query + "WHERE rec.id = :id")
    LiveData<Recording> loadRecording(int id);

    @Query(query + "WHERE rec.id = :id")
    Recording loadRecordingSync(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Recording> recordings);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Recording recording);

    @Update
    void update(Recording... recording);

    @Delete
    void delete(Recording recording);

    @Query("DELETE FROM recordings WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM recordings")
    void deleteAll();
}
