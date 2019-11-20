package org.tvheadend.tvhclient.data.dao

import androidx.room.*
import org.tvheadend.tvhclient.data.entity.ServerProfile

@Dao
interface ServerProfileDao {

    @get:Query("SELECT COUNT (*) FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE")
    val itemCountSync: Int

    @Query("SELECT p.* FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND p.type = 'htsp_playback'")
    fun loadHtspPlaybackProfilesSync(): List<ServerProfile>

    @Query("SELECT p.* FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND p.type = 'http_playback'")
    fun loadHttpPlaybackProfilesSync(): List<ServerProfile>

    @Query("SELECT p.* FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND p.type = 'recording'")
    fun loadAllRecordingProfilesSync(): List<ServerProfile>

    @Insert
    fun insert(serverProfile: ServerProfile)

    @Update
    fun update(serverProfile: ServerProfile)

    @Delete
    fun delete(serverProfile: ServerProfile)

    @Query("DELETE FROM server_profiles")
    fun deleteAll()

    @Query("SELECT p.* FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND p.id = :id")
    fun loadProfileByIdSync(id: Int): ServerProfile

    @Query("SELECT p.* FROM server_profiles AS p " +
            " WHERE $CONNECTION_IS_ACTIVE" +
            " AND p.uuid = :uuid")
    fun loadProfileByUuidSync(uuid: String): ServerProfile

    companion object {

        const val CONNECTION_IS_ACTIVE = " p.connection_id IN (SELECT id FROM connections WHERE active = 1) "
    }
}
