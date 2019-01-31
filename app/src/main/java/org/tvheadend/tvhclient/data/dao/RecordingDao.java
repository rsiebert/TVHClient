package org.tvheadend.tvhclient.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import org.tvheadend.tvhclient.data.entity.Recording;

import java.util.List;

@Dao
public abstract class RecordingDao {

    private final String RECORDING_BASE_QUERY = "SELECT DISTINCT rec.*, " +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM recordings AS rec " +
            "LEFT JOIN channels AS c ON c.id = rec.channel_id ";

    private final String CONNECTION_IS_ACTIVE = " rec.connection_id IN (SELECT id FROM connections WHERE active = 1) ";

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            "ORDER BY rec.start DESC")
    public abstract LiveData<List<Recording>> loadAllRecordings();

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND rec.error IS NULL AND rec.state = 'completed'" +
            "ORDER BY rec.start DESC")
    public abstract LiveData<List<Recording>> loadAllCompletedRecordings();

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND rec.error IS NULL AND (rec.state = 'recording' OR rec.state = 'scheduled')" +
            "ORDER BY rec.start ASC")
    public abstract LiveData<List<Recording>> loadAllScheduledRecordings();

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND (rec.error IS NOT NULL AND (rec.state='missed'  OR rec.state='invalid')) " +
            " OR (rec.error IS NULL  AND rec.state='missed') " +
            " OR (rec.error='Aborted by user' AND rec.state='completed')" +
            "ORDER BY rec.start DESC")
    public abstract LiveData<List<Recording>> loadAllFailedRecordings();

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND rec.error = 'File missing' AND rec.state = 'completed'" +
            "ORDER BY rec.start DESC")
    public abstract LiveData<List<Recording>> loadAllRemovedRecordings();

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND rec.id = :id")
    public abstract LiveData<Recording> loadRecordingById(int id);

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND rec.id = :id")
    public abstract Recording loadRecordingByIdSync(int id);

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND rec.channel_id = :channelId")
    public abstract LiveData<List<Recording>> loadAllRecordingsByChannelId(int channelId);

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            "WHERE " + CONNECTION_IS_ACTIVE +
            " AND rec.event_id = :id")
    public abstract Recording loadRecordingByEventIdSync(int id);

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(Recording recording);

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(List<Recording> recordings);

    @Update
    public abstract void update(Recording recording);

    @Delete
    public abstract void delete(Recording recording);

    @Delete
    public abstract void delete(List<Recording> recordings);

    @Transaction
    public void insertAndDelete(List<Recording> newRecordings, List<Recording> oldRecordings) {
        delete(oldRecordings);
        insert(newRecordings);
    }

    @Query("DELETE FROM recordings " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND id = :id")
    public abstract void deleteById(int id);

    @Query("DELETE FROM recordings")
    public abstract void deleteAll();

    @Query("SELECT COUNT (*) FROM recordings AS rec " +
            "WHERE (rec.error IS NULL AND rec.state = 'completed') " +
            "AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    public abstract LiveData<Integer> getCompletedRecordingCount();

    @Query("SELECT COUNT (*) FROM recordings AS rec " +
            "WHERE (rec.error IS NULL AND (rec.state = 'recording' OR rec.state = 'scheduled')) " +
            "AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    public abstract LiveData<Integer> getScheduledRecordingCount();

    @Query("SELECT COUNT (*) FROM recordings AS rec " +
            "WHERE ((rec.error IS NOT NULL AND (rec.state='missed'  OR rec.state='invalid')) " +
            " OR (rec.error IS NULL  AND rec.state='missed') " +
            " OR (rec.error='Aborted by user' AND rec.state='completed')) " +
            "AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    public abstract LiveData<Integer> getFailedRecordingCount();

    @Query("SELECT COUNT (*) FROM recordings AS rec " +
            "WHERE (rec.error = 'File missing' AND rec.state = 'completed') " +
            "AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    public abstract LiveData<Integer> getRemovedRecordingCount();

    @Query("SELECT COUNT (*) FROM recordings AS rec " +
            "WHERE " + CONNECTION_IS_ACTIVE)
    public abstract int getItemCountSync();
}
