package org.tvheadend.tvhclient.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.tvheadend.tvhclient.domain.entity.ServerStatus

@Dao
interface ServerStatusDao {

    @Query("DELETE FROM server_status WHERE connection_id = :id")
    fun deleteByConnectionId(id: Int)

    @Query("SELECT s.*, " +
            "c.name AS connection_name " +
            "FROM server_status AS s " +
            "JOIN connections AS c ON c.id = s.connection_id AND c.active = 1")
    fun loadActiveServerStatusSync(): ServerStatus?

    @Query("SELECT s.*, " +
            "c.name AS connection_name " +
            "FROM server_status AS s " +
            "JOIN connections AS c ON c.id = s.connection_id AND c.active = 1")
    fun loadActiveServerStatus(): LiveData<ServerStatus>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(serverStatus: ServerStatus)

    @Update
    fun update(serverStatus: ServerStatus)

    @Delete
    fun delete(serverStatus: ServerStatus)

    @Query("DELETE FROM server_status")
    fun deleteAll()

    @Query("SELECT s.*, " +
            "c.name AS connection_name " +
            "FROM server_status AS s " +
            "LEFT JOIN connections AS c ON c.id = s.connection_id " +
            "WHERE s.connection_id = :id")
    fun loadServerStatusByIdSync(id: Int): ServerStatus

    @Query("SELECT s.*, " +
            "c.name AS connection_name " +
            "FROM server_status AS s " +
            "LEFT JOIN connections AS c ON c.id = s.connection_id " +
            "WHERE s.connection_id = :id")
    fun loadServerStatusById(id: Int): LiveData<ServerStatus>
}
