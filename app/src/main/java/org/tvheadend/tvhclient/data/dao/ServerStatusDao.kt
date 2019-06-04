package org.tvheadend.tvhclient.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.tvheadend.tvhclient.domain.entity.ServerStatus

@Dao
interface ServerStatusDao {

    @Query("DELETE FROM server_status WHERE connection_id = :id")
    fun deleteByConnectionId(id: Int)

    @Transaction
    @Query("$SERVER_STATUS_BASE_QUERY WHERE $CONNECTION_IS_ACTIVE")
    fun loadAllServerStatus(): LiveData<List<ServerStatus>>

    @Query("$SERVER_STATUS_BASE_QUERY WHERE $CONNECTION_IS_ACTIVE")
    fun loadActiveServerStatusSync(): ServerStatus

    @Query("$SERVER_STATUS_BASE_QUERY WHERE $CONNECTION_IS_ACTIVE")
    fun loadActiveServerStatus(): LiveData<ServerStatus>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(serverStatus: ServerStatus)

    @Update
    fun update(serverStatus: ServerStatus)

    @Delete
    fun delete(serverStatus: ServerStatus)

    @Query("DELETE FROM server_status")
    fun deleteAll()

    @Query("$SERVER_STATUS_BASE_QUERY WHERE s.connection_id = :id")
    fun loadServerStatusByIdSync(id: Int): ServerStatus

    @Query("$SERVER_STATUS_BASE_QUERY WHERE s.connection_id = :id")
    fun loadServerStatusById(id: Int): LiveData<ServerStatus>

    @get:Query("SELECT COUNT (*) FROM server_status AS s WHERE $CONNECTION_IS_ACTIVE")
    val serverStatusCount: LiveData<Int>

    companion object {

        const val SERVER_STATUS_BASE_QUERY = "SELECT DISTINCT s.* FROM server_status AS s "

        const val CONNECTION_IS_ACTIVE = " s.connection_id IN (SELECT id FROM connections WHERE active = 1) "
    }
}
