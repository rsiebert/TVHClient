package org.tvheadend.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.tvheadend.data.entity.SeriesRecording

@Dao
interface SeriesRecordingDao {

    @get:Query("SELECT COUNT (*) FROM series_recordings AS rec " +
            " WHERE $CONNECTION_IS_ACTIVE")
    val itemCount: LiveData<Int>

    @get:Query("SELECT COUNT (*) FROM series_recordings AS rec " +
            " WHERE $CONNECTION_IS_ACTIVE")
    val itemCountSync: Int

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE")
    fun loadAllRecordings(): LiveData<List<SeriesRecording>>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.id = :id")
    fun loadRecordingById(id: String): LiveData<SeriesRecording>

    @Transaction
    @Query(RECORDING_BASE_QUERY +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND rec.id = :id")
    fun loadRecordingByIdSync(id: String): SeriesRecording

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(recording: SeriesRecording)

    @Update
    fun update(recording: SeriesRecording)

    @Delete
    fun delete(recording: SeriesRecording)

    @Query("DELETE FROM series_recordings " +
            " WHERE connection_id IN (SELECT id FROM connections WHERE active = 1)" +
            " AND id = :id ")
    fun deleteById(id: String)

    @Query("DELETE FROM series_recordings")
    fun deleteAll()

    companion object {

        const val RECORDING_BASE_QUERY = "SELECT DISTINCT rec.*, " +
                "c.name AS channel_name, " +
                "c.icon AS channel_icon " +
                "FROM series_recordings AS rec " +
                "LEFT JOIN channels AS c ON  c.id = rec.channel_id "

        const val CONNECTION_IS_ACTIVE = " rec.connection_id IN (SELECT id FROM connections WHERE active = 1) "
    }
}
