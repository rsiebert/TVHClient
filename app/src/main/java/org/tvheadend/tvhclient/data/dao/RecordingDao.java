package org.tvheadend.tvhclient.data.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.entity.Recording;

import java.util.List;

@Dao
public interface RecordingDao {

    @Transaction
    @Query("SELECT rec.*, " +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM recordings AS rec " +
            "LEFT JOIN channels AS c ON c.id = rec.channel_id " +
            "WHERE rec.connection_id IN (SELECT id FROM connections WHERE active = 1) ")
    LiveData<List<Recording>> loadAllRecordings();

    @Transaction
    @Query("SELECT rec.*, " +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM recordings AS rec " +
            "LEFT JOIN channels AS c ON c.id = rec.channel_id " +
            "WHERE rec.connection_id IN (SELECT id FROM connections WHERE active = 1) ")
    List<Recording> loadAllRecordingsSync();

    @Transaction
    @Query("SELECT rec.*, " +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM recordings AS rec " +
            "LEFT JOIN channels AS c ON c.id = rec.channel_id " +
            "WHERE rec.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND rec.error IS NULL AND rec.state = 'completed'")
    LiveData<List<Recording>> loadAllCompletedRecordings();

    @Transaction
    @Query("SELECT rec.*, " +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM recordings AS rec " +
            "LEFT JOIN channels AS c ON c.id = rec.channel_id " +
            "WHERE rec.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND rec.error IS NULL AND (rec.state = 'recording' OR rec.state = 'scheduled')")
    LiveData<List<Recording>> loadAllScheduledRecordings();

    @Transaction
    @Query("SELECT rec.*, " +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM recordings AS rec " +
            "LEFT JOIN channels AS c ON c.id = rec.channel_id " +
            "WHERE rec.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND (rec.error IS NOT NULL AND (rec.state='missed'  OR rec.state='invalid')) " +
            " OR (rec.error IS NULL  AND rec.state='missed') " +
            " OR (rec.error='Aborted by user' AND rec.state='completed')")
    LiveData<List<Recording>> loadAllFailedRecordings();

    @Transaction
    @Query("SELECT rec.*, " +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM recordings AS rec " +
            "LEFT JOIN channels AS c ON c.id = rec.channel_id " +
            "WHERE rec.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND rec.error = 'File missing' AND rec.state = 'completed'")
    LiveData<List<Recording>> loadAllRemovedRecordings();

    @Transaction
    @Query("SELECT rec.*, " +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM recordings AS rec " +
            "LEFT JOIN channels AS c ON c.id = rec.channel_id " +
            "WHERE rec.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND rec.id = :id")
    LiveData<Recording> loadRecordingById(int id);

    @Transaction
    @Query("SELECT rec.*, " +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM recordings AS rec " +
            "LEFT JOIN channels AS c ON c.id = rec.channel_id " +
            "WHERE rec.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND rec.id = :id")
    Recording loadRecordingByIdSync(int id);

    @Transaction
    @Query("SELECT rec.*, " +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM recordings AS rec " +
            "LEFT JOIN channels AS c ON c.id = rec.channel_id " +
            "WHERE rec.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND rec.channel_id = :channelId")
    LiveData<List<Recording>> loadAllRecordingsByChannelId(int channelId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Recording> recordings);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Recording recording);

    @Update
    void update(Recording... recording);

    @Delete
    void delete(Recording recording);

    @Query("DELETE FROM recordings " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND id = :id")
    void deleteById(int id);

    @Query("DELETE FROM recordings")
    void deleteAll();

    @Transaction
    @Query("SELECT rec.*, " +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM recordings AS rec " +
            "LEFT JOIN channels AS c ON c.id = rec.channel_id " +
            "WHERE rec.connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND rec.event_id = :id")
    Recording loadRecordingByEventIdSync(int id);

    @Query("SELECT COUNT (*) FROM recordings AS rec " +
            "WHERE (rec.error IS NULL AND rec.state = 'completed') " +
            "AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    LiveData<Integer> getCompletedRecordingCount();

    @Query("SELECT COUNT (*) FROM recordings AS rec " +
            "WHERE (rec.error IS NULL AND (rec.state = 'recording' OR rec.state = 'scheduled')) " +
            "AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    LiveData<Integer> getScheduledRecordingCount();

    @Query("SELECT COUNT (*) FROM recordings AS rec " +
            "WHERE ((rec.error IS NOT NULL AND (rec.state='missed'  OR rec.state='invalid')) " +
            " OR (rec.error IS NULL  AND rec.state='missed') " +
            " OR (rec.error='Aborted by user' AND rec.state='completed')) " +
            "AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    LiveData<Integer> getFailedRecordingCount();

    @Query("SELECT COUNT (*) FROM recordings AS rec " +
            "WHERE (rec.error = 'File missing' AND rec.state = 'completed') " +
            "AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    LiveData<Integer> getRemovedRecordingCount();
}
