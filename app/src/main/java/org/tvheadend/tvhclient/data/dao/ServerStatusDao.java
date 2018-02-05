package org.tvheadend.tvhclient.data.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import org.tvheadend.tvhclient.data.entity.ServerStatus;

@Dao
public interface ServerStatusDao {

    @Query("DELETE FROM server_status WHERE connection_id = :id")
    void deleteByConnectionId(int id);

    // Load the server status of the currently active connection
    @Query("SELECT s.*, " +
            "c.name AS connection_name " +
            "FROM server_status AS s " +
            "LEFT JOIN connections AS c ON c.id = s.connection_id AND c.active = 1")
    ServerStatus loadServerStatusSync();

    // Update the playback profile in the server status row
    // which belongs to the currently active connection
    @Query("UPDATE server_status " +
            "SET playback_server_profile_id = :id " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1)")
    void updatePlaybackServerProfile(int id);

    @Query("UPDATE server_status " +
            "SET recording_server_profile_id = :id " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1)")
    void updateRecordingServerProfile(int id);

    @Query("UPDATE server_status " +
            "SET casting_server_profile_id = :id " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1)")
    void updateCastingServerProfile(int id);

    @Query("UPDATE server_status " +
            "SET playback_transcoding_profile_id = :id " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1)")
    void updatePlaybackTranscodingProfile(int id);

    @Query("UPDATE server_status " +
            "SET recording_transcoding_profile_id = :id " +
            "WHERE connection_id IN (SELECT id FROM connections WHERE active = 1)")
    void updateRecordingTranscodingProfile(int id);

    @Insert
    void insert(ServerStatus serverStatus);

    @Update
    void update(ServerStatus serverStatus);

    @Delete
    void delete(ServerStatus serverStatus);
}
