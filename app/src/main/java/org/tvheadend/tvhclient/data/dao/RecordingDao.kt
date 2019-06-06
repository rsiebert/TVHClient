package org.tvheadend.tvhclient.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.tvheadend.tvhclient.domain.entity.Recording

@Dao
abstract class RecordingDao {

    @get:Query("SELECT COUNT (*) FROM recordings AS rec " +
            " WHERE (rec.error IS NULL AND rec.state = 'completed') " +
            " AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    abstract val completedRecordingCount: LiveData<Int>

    @get:Query("SELECT COUNT (*) FROM recordings AS rec " +
            " WHERE (rec.error IS NULL AND (rec.state = 'recording' OR rec.state = 'scheduled')) " +
            " AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    abstract val scheduledRecordingCount: LiveData<Int>

    @get:Query("SELECT COUNT (*) FROM recordings AS rec " +
            " WHERE (rec.error IS NULL AND (rec.state = 'recording')) " +
            " AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    abstract val runningRecordingCount: LiveData<Int>

    @get:Query("SELECT COUNT (*) FROM recordings AS rec " +
            " WHERE ((rec.error IS NOT NULL AND (rec.state='missed'  OR rec.state='invalid')) " +
            " OR (rec.error IS NULL  AND rec.state='missed') " +
            " OR (rec.error='Aborted by user' AND rec.state='completed')) " +
            " AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    abstract val failedRecordingCount: LiveData<Int>

    @get:Query("SELECT COUNT (*) FROM recordings AS rec " +
            " WHERE (rec.error = 'File missing' AND rec.state = 'completed') " +
            " AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    abstract val removedRecordingCount: LiveData<Int>

    @get:Query("SELECT COUNT (*) FROM recordings AS rec " +
            " WHERE $CONNECTION_IS_ACTIVE")
    abstract val itemCountSync: Int

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " ORDER BY rec.start DESC")
    abstract fun loadRecordings(): LiveData<List<Recording>>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.error IS NULL AND rec.state = 'completed'" +
            " ORDER BY rec.start DESC")
    abstract fun loadCompletedRecordings(): LiveData<List<Recording>>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.error IS NULL AND (rec.state = 'recording' OR rec.state = 'scheduled')" +
            " AND rec.duplicate = 0 " +
            " ORDER BY rec.start ASC")
    abstract fun loadUniqueScheduledRecordings(): LiveData<List<Recording>>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.error IS NULL AND (rec.state = 'recording' OR rec.state = 'scheduled')" +
            " ORDER BY rec.start ASC")
    abstract fun loadScheduledRecordings(): LiveData<List<Recording>>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND (rec.error IS NOT NULL AND (rec.state='missed'  OR rec.state='invalid')) " +
            " OR (rec.error IS NULL  AND rec.state='missed') " +
            " OR (rec.error='Aborted by user' AND rec.state='completed')" +
            " ORDER BY rec.start DESC")
    abstract fun loadFailedRecordings(): LiveData<List<Recording>>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.error = 'File missing' AND rec.state = 'completed'" +
            " ORDER BY rec.start DESC")
    abstract fun loadRemovedRecordings(): LiveData<List<Recording>>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.id = :id")
    abstract fun loadRecordingById(id: Int): LiveData<Recording>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.id = :id")
    abstract fun loadRecordingByIdSync(id: Int): Recording

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.channel_id = :channelId")
    abstract fun loadRecordingsByChannelId(channelId: Int): LiveData<List<Recording>>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.event_id = :id")
    abstract fun loadRecordingByEventIdSync(id: Int): Recording

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(recording: Recording)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(recordings: List<Recording>)

    @Update
    abstract fun update(recording: Recording)

    @Delete
    abstract fun delete(recording: Recording)

    @Delete
    abstract fun delete(recordings: List<Recording>)

    @Query("DELETE FROM recordings " +
            " WHERE connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND id = :id")
    abstract fun deleteById(id: Int)

    @Query("DELETE FROM recordings")
    abstract fun deleteAll()

    companion object {
        const val RECORDING_BASE_QUERY = "SELECT DISTINCT rec.*, " +
                "c.name AS channel_name, " +
                "c.icon AS channel_icon " +
                "FROM recordings AS rec " +
                "LEFT JOIN channels AS c ON c.id = rec.channel_id "

        const val CONNECTION_IS_ACTIVE = " rec.connection_id IN (SELECT id FROM connections WHERE active = 1) "
    }

}
