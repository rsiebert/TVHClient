package org.tvheadend.tvhclient.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import org.tvheadend.tvhclient.domain.entity.SeriesRecording;

import java.util.List;

@Dao
public interface SeriesRecordingDao {

    String RECORDING_BASE_QUERY = "SELECT DISTINCT rec.*, " +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM series_recordings AS rec " +
            "LEFT JOIN channels AS c ON  c.id = rec.channel_id ";

    String CONNECTION_IS_ACTIVE = " rec.connection_id IN (SELECT id FROM connections WHERE active = 1) ";

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE)
    LiveData<List<SeriesRecording>> loadAllRecordings();

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND rec.id = :id")
    LiveData<SeriesRecording> loadRecordingById(String id);

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND rec.id = :id")
    SeriesRecording loadRecordingByIdSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SeriesRecording recording);

    @Update
    void update(SeriesRecording recording);

    @Delete
    void delete(SeriesRecording recording);

    @Query("DELETE FROM series_recordings " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1)" +
            " AND id = :id ")
    void deleteById(String id);

    @Query("DELETE FROM series_recordings")
    void deleteAll();

    @Query("SELECT COUNT (*) FROM series_recordings AS rec " +
            "WHERE " + CONNECTION_IS_ACTIVE)
    LiveData<Integer> getRecordingCount();
}
