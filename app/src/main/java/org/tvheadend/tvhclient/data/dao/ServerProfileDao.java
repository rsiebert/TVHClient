package org.tvheadend.tvhclient.data.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.entity.ServerProfile;

import java.util.List;

@Dao
public interface ServerProfileDao {

    @Query("SELECT p.* FROM server_profiles AS p " +
            "LEFT JOIN server_status AS s ON s.playback_server_profile_id = p.id " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1")
    ServerProfile loadPlaybackProfileSync();

    @Query("SELECT p.* FROM server_profiles AS p " +
            "LEFT JOIN server_status AS s ON s.recording_server_profile_id = p.id " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1")
    ServerProfile loadRecordingProfileSync();

    @Query("SELECT p.* FROM server_profiles AS p " +
            "LEFT JOIN server_status AS s ON s.casting_server_profile_id = p.id " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1")
    ServerProfile loadCastingProfileSync();

    @Query("SELECT p.* FROM server_profiles AS p " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1 " +
            "WHERE p.type = 'playback'")
    List<ServerProfile> loadAllPlaybackProfilesSync();

    @Query("SELECT p.* FROM server_profiles AS p " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1 " +
            "WHERE p.type = 'recording'")
    List<ServerProfile> loadAllRecordingProfilesSync();

    @Insert
    long insert(ServerProfile serverProfile);

    @Update
    void update(ServerProfile... serverProfile);

    @Delete
    void delete(ServerProfile serverProfile);

    @Query("SELECT p.* FROM server_profiles AS p " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1 " +
            "WHERE p.id = :id")
    ServerProfile loadProfileByIdSync(int id);

    @Query("SELECT p.* FROM server_profiles AS p " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1 " +
            "WHERE p.uuid = :uuid")
    ServerProfile loadProfileByUuidSync(String uuid);
}
