package org.tvheadend.tvhclient.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import org.tvheadend.tvhclient.data.entity.ServerProfile;

import java.util.List;

@Dao
public interface ServerProfileDao {

    @Query("SELECT p.* FROM server_profiles AS p " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1 " +
            "WHERE p.type = 'htsp_playback'")
    List<ServerProfile> loadHtspPlaybackProfilesSync();

    @Query("SELECT p.* FROM server_profiles AS p " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1 " +
            "WHERE p.type = 'http_playback'")
    List<ServerProfile> loadHttpPlaybackProfilesSync();

    @Query("SELECT p.* FROM server_profiles AS p " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1 " +
            "WHERE p.type = 'recording'")
    List<ServerProfile> loadAllRecordingProfilesSync();

    @Insert
    void insert(ServerProfile serverProfile);

    @Update
    void update(ServerProfile serverProfile);

    @Delete
    void delete(ServerProfile serverProfile);

    @Query("DELETE FROM server_profiles")
    void deleteAll();

    @Query("SELECT p.* FROM server_profiles AS p " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1 " +
            "WHERE p.id = :id")
    ServerProfile loadProfileByIdSync(int id);

    @Query("SELECT p.* FROM server_profiles AS p " +
            "LEFT JOIN connections AS c ON c.id = p.connection_id AND c.active = 1 " +
            "WHERE p.uuid = :uuid")
    ServerProfile loadProfileByUuidSync(String uuid);
}
