package org.tvheadend.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.tvheadend.data.entity.RecordingEntity

@Dao
internal interface RecordingDao {

    @get:Query("SELECT COUNT (*) FROM recordings AS rec " +
            " WHERE (rec.error IS NULL AND rec.state = 'completed') " +
            " AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    val completedRecordingCount: LiveData<Int>

    @get:Query("SELECT COUNT (*) FROM recordings AS rec " +
            " WHERE (rec.error IS NULL AND (rec.state = 'recording' OR rec.state = 'scheduled')) " +
            " AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    val scheduledRecordingCount: LiveData<Int>

    @get:Query("SELECT COUNT (*) FROM recordings AS rec " +
            " WHERE (rec.error IS NULL AND (rec.state = 'recording')) " +
            " AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    val runningRecordingCount: LiveData<Int>

    @get:Query("SELECT COUNT (*) FROM recordings AS rec " +
            " WHERE ((rec.error IS NOT NULL AND (rec.state='missed'  OR rec.state='invalid')) " +
            " OR (rec.error IS NULL  AND rec.state='missed') " +
            " OR (rec.error='Aborted by user' AND rec.state='completed')) " +
            " AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    val failedRecordingCount: LiveData<Int>

    @get:Query("SELECT COUNT (*) FROM recordings AS rec " +
            " WHERE (rec.error = 'File missing' AND rec.state = 'completed') " +
            " AND rec.connection_id IN (SELECT id FROM connections WHERE active = 1)")
    val removedRecordingCount: LiveData<Int>

    @get:Query("SELECT COUNT (*) FROM recordings AS rec " +
            " WHERE $CONNECTION_IS_ACTIVE")
    val itemCountSync: Int

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " ORDER BY rec.start DESC")
    fun loadRecordings(): LiveData<List<RecordingEntity>>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.error IS NULL AND rec.state = 'completed' " +
            ORDER_BY)
    fun loadCompletedRecordings(sortOrder: Int): LiveData<List<RecordingEntity>>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.error IS NULL AND (rec.state = 'recording' OR rec.state = 'scheduled')" +
            " AND rec.duplicate = 0 " +
            " ORDER BY rec.start ASC")
    fun loadUniqueScheduledRecordings(): LiveData<List<RecordingEntity>>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.error IS NULL AND (rec.state = 'recording' OR rec.state = 'scheduled')" +
            " ORDER BY rec.start ASC")
    fun loadScheduledRecordings(): LiveData<List<RecordingEntity>>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND (rec.error IS NOT NULL AND (rec.state='missed'  OR rec.state='invalid')) " +
            " OR (rec.error IS NULL  AND rec.state='missed') " +
            " OR (rec.error='Aborted by user' AND rec.state='completed')" +
            " ORDER BY rec.start DESC")
    fun loadFailedRecordings(): LiveData<List<RecordingEntity>>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.error = 'File missing' AND rec.state = 'completed'" +
            " ORDER BY rec.start DESC")
    fun loadRemovedRecordings(): LiveData<List<RecordingEntity>>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.id = :id")
    fun loadRecordingById(id: Int): LiveData<RecordingEntity>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.id = :id")
    fun loadRecordingByIdSync(id: Int): RecordingEntity

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.channel_id = :channelId")
    fun loadRecordingsByChannelId(channelId: Int): LiveData<List<RecordingEntity>>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.event_id = :id")
    fun loadRecordingByEventIdSync(id: Int): RecordingEntity?

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(recording: RecordingEntity)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(recordings: List<RecordingEntity>)

    @Update
    fun update(recording: RecordingEntity)

    @Delete
    fun delete(recording: RecordingEntity)

    @Delete
    fun delete(recordings: List<RecordingEntity>)

    @Query("DELETE FROM recordings " +
            " WHERE connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND id = :id")
    fun deleteById(id: Int)

    @Query("DELETE FROM recordings")
    fun deleteAll()

    companion object {
        const val RECORDING_BASE_QUERY = "SELECT DISTINCT rec.*, " +
                "c.name AS channel_name, " +
                "c.icon AS channel_icon " +
                "FROM recordings AS rec " +
                "LEFT JOIN channels AS c ON c.id = rec.channel_id "

        const val ORDER_BY = " ORDER BY " +
                "CASE :sortOrder WHEN 0 THEN rec.start END ASC," +
                "CASE :sortOrder WHEN 1 THEN rec.start END DESC," +
                "CASE :sortOrder WHEN 2 THEN rec.title END ASC," +
                "CASE :sortOrder WHEN 3 THEN rec.title END DESC," +
                "CASE :sortOrder WHEN 4 THEN rec.content_type END ASC," +
                "CASE :sortOrder WHEN 5 THEN rec.content_type END DESC," +
                "CASE :sortOrder WHEN 6 THEN rec.duration END ASC," +
                "CASE :sortOrder WHEN 7 THEN rec.duration END DESC"

        const val CONNECTION_IS_ACTIVE = " rec.connection_id IN (SELECT id FROM connections WHERE active = 1) "
    }

}
