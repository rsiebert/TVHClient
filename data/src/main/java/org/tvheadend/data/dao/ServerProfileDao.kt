package org.tvheadend.data.dao

import androidx.room.*
import org.tvheadend.data.entity.ServerProfile.Companion.HTSP_PROFILE
import org.tvheadend.data.entity.ServerProfile.Companion.HTTP_PROFILE
import org.tvheadend.data.entity.ServerProfile.Companion.RECORDING_PROFILE
import org.tvheadend.data.entity.ServerProfileEntity

@Dao
interface ServerProfileDao {

    @get:Query("SELECT COUNT (*) FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE")
    val itemCountSync: Int

    @Query("SELECT p.* FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND p.type = '" + HTSP_PROFILE + "'")
    fun loadHtspPlaybackProfilesSync(): List<ServerProfileEntity>

    @Query("SELECT p.* FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND p.type = '" + HTTP_PROFILE + "'")
    fun loadHttpPlaybackProfilesSync(): List<ServerProfileEntity>

    @Query("SELECT p.* FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND p.type = '" + RECORDING_PROFILE + "'")
    fun loadAllRecordingProfilesSync(): List<ServerProfileEntity>

    @Insert
    fun insert(serverProfile: ServerProfileEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(serverProfiles: List<ServerProfileEntity>)

    @Update
    fun update(serverProfile: ServerProfileEntity)

    @Delete
    fun delete(serverProfile: ServerProfileEntity)

    @Query("DELETE FROM server_profiles")
    fun deleteAll()

    @Query("SELECT p.* FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND p.id = :id")
    fun loadProfileByIdSync(id: Int): ServerProfileEntity?

    @Query("SELECT p.* FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND p.uuid = :uuid")
    fun loadProfileByUuidSync(uuid: String): ServerProfileEntity?

    companion object {

        const val CONNECTION_IS_ACTIVE = " p.connection_id IN (SELECT id FROM connections WHERE active = 1) "
    }
}
