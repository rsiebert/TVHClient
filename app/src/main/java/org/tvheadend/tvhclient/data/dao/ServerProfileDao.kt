package org.tvheadend.tvhclient.data.dao

import androidx.room.*
import org.tvheadend.tvhclient.domain.entity.ServerProfile

@Dao
interface ServerProfileDao {

    @Query("SELECT p.* FROM server_profiles AS p " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1 " +
            "WHERE p.type = 'htsp_playback'")
    fun loadHtspPlaybackProfilesSync(): List<ServerProfile>

    @Query("SELECT p.* FROM server_profiles AS p " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1 " +
            "WHERE p.type = 'http_playback'")
    fun loadHttpPlaybackProfilesSync(): List<ServerProfile>

    @Query("SELECT p.* FROM server_profiles AS p " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1 " +
            "WHERE p.type = 'recording'")
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
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1 " +
            "WHERE p.id = :id")
    fun loadProfileByIdSync(id: Int): ServerProfile

    @Query("SELECT p.* FROM server_profiles AS p " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1 " +
            "WHERE p.uuid = :uuid")
    fun loadProfileByUuidSync(uuid: String): ServerProfile
}
