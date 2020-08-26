package org.tvheadend.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.tvheadend.data.entity.TimerRecordingEntity

@Dao
internal interface TimerRecordingDao {

    @get:Query("SELECT COUNT (*) FROM timer_recordings AS rec " +
            " WHERE $CONNECTION_IS_ACTIVE")
    val itemCount: LiveData<Int>

    @get:Query("SELECT COUNT (*) FROM timer_recordings AS rec " +
            " WHERE $CONNECTION_IS_ACTIVE")
    val itemCountSync: Int

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " ORDER BY rec.start, rec.title ASC")
    fun loadAllRecordings(): LiveData<List<TimerRecordingEntity>>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.id = :id")
    fun loadRecordingById(id: String): LiveData<TimerRecordingEntity>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.id = :id")
    fun loadRecordingByIdSync(id: String): TimerRecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(recording: TimerRecordingEntity)

    @Update
    fun update(recording: TimerRecordingEntity)

    @Delete
    fun delete(recording: TimerRecordingEntity)

    @Query("DELETE FROM timer_recordings " +
            " WHERE connection_id IN (SELECT id FROM connections WHERE active = 1) " +
            " AND id = :id")
    fun deleteById(id: String)

    @Query("DELETE FROM timer_recordings")
    fun deleteAll()

    companion object {

        const val RECORDING_BASE_QUERY = "SELECT DISTINCT rec.*, " +
                "c.name AS channel_name, " +
                "c.icon AS channel_icon " +
                "FROM timer_recordings AS rec " +
                "LEFT JOIN channels AS c ON  c.id = rec.channel_id "

        const val CONNECTION_IS_ACTIVE = " rec.connection_id IN (SELECT id FROM connections WHERE active = 1) "
    }
}
