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

    @Transaction
    @Query("SELECT rec.*, " +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM timer_recordings AS rec " +
            "LEFT JOIN channels AS c ON  c.id = rec.channel_id " +
            "WHERE rec.connection_id IN (SELECT id FROM connections WHERE active = 1) ")
    LiveData<List<TimerRecording>> loadAllRecordings();

    @Transaction
    @Query("SELECT rec.*, " +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM timer_recordings AS rec " +
            "LEFT JOIN channels AS c ON  c.id = rec.channel_id " +
            "WHERE rec.id = :id " +
            " AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    LiveData<TimerRecording> loadRecordingById(String id);

    @Transaction
    @Query("SELECT rec.*, " +
            "c.name AS channel_name, " +
            "c.icon AS channel_icon " +
            "FROM timer_recordings AS rec " +
            "LEFT JOIN channels AS c ON  c.id = rec.channel_id " +
            "WHERE rec.id = :id " +
            " AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    TimerRecording loadRecordingByIdSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TimerRecording recording);

    @Update
    void update(TimerRecording... recording);

    @Delete
    void delete(TimerRecording recording);

    @Query("DELETE FROM timer_recordings " +
            "WHERE id = :id " +
            " AND connection_id IN (SELECT id FROM connections WHERE active = 1)")
    void deleteById(String id);

    @Query("DELETE FROM timer_recordings")
    void deleteAll();

    @Query("SELECT COUNT (*) FROM timer_recordings " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1)")
    LiveData<Integer> getRecordingCount();
}
