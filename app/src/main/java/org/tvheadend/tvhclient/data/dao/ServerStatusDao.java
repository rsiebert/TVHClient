package org.tvheadend.tvhclient.data.dao;

import android.arch.lifecycle.LiveData;
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

    @Query("SELECT s.*, " +
            "c.name AS connection_name " +
            "FROM server_status AS s " +
            "LEFT JOIN connections AS c ON c.id = s.connection_id AND c.active = 1")
    ServerStatus loadServerStatusSync();

    @Query("SELECT s.*, " +
            "c.name AS connection_name " +
            "FROM server_status AS s " +
            "LEFT JOIN connections AS c ON c.id = s.connection_id AND c.active = 1")
    LiveData<ServerStatus> loadServerStatus();

    @Insert
    void insert(ServerStatus serverStatus);

    @Update
    void update(ServerStatus serverStatus);

    @Delete
    void delete(ServerStatus serverStatus);

    @Query("DELETE FROM server_status")
    void deleteAll();

    @Query("SELECT s.*, " +
            "c.name AS connection_name " +
            "FROM server_status AS s " +
            "LEFT JOIN connections AS c ON c.id = s.connection_id " +
            "WHERE s.connection_id = :id")
    ServerStatus loadServerStatusByIdSync(int id);

    @Query("SELECT s.*, " +
            "c.name AS connection_name " +
            "FROM server_status AS s " +
            "LEFT JOIN connections AS c ON c.id = s.connection_id " +
            "WHERE s.connection_id = :id")
    LiveData<ServerStatus> loadServerStatusById(int id);
}
