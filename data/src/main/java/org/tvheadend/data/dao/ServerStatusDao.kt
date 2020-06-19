package org.tvheadend.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.tvheadend.data.entity.ServerStatusEntity

@Dao
internal interface ServerStatusDao {

    @Query("DELETE FROM server_status WHERE connection_id = :id")
    fun deleteByConnectionId(id: Int)

    @Transaction
    @Query("$SERVER_STATUS_BASE_QUERY WHERE $CONNECTION_IS_ACTIVE")
    fun loadAllServerStatus(): LiveData<List<ServerStatusEntity>>

    @Query("$SERVER_STATUS_BASE_QUERY WHERE $CONNECTION_IS_ACTIVE")
    fun loadActiveServerStatusSync(): ServerStatusEntity?

    @Query("$SERVER_STATUS_BASE_QUERY WHERE $CONNECTION_IS_ACTIVE")
    fun loadActiveServerStatus(): LiveData<ServerStatusEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(serverStatus: ServerStatusEntity)

    @Update
    fun update(serverStatus: ServerStatusEntity)

    @Delete
    fun delete(serverStatus: ServerStatusEntity)

    @Query("DELETE FROM server_status")
    fun deleteAll()

    @Query("$SERVER_STATUS_BASE_QUERY WHERE s.connection_id = :id")
    fun loadServerStatusByIdSync(id: Int): ServerStatusEntity?

    @Query("$SERVER_STATUS_BASE_QUERY WHERE s.connection_id = :id")
    fun loadServerStatusById(id: Int): LiveData<ServerStatusEntity>

    @get:Query("SELECT COUNT (*) FROM server_status AS s WHERE $CONNECTION_IS_ACTIVE")
    val serverStatusCount: LiveData<Int>

    companion object {

        const val SERVER_STATUS_BASE_QUERY = "SELECT DISTINCT s.* FROM server_status AS s "

        const val CONNECTION_IS_ACTIVE = " s.connection_id IN (SELECT id FROM connections WHERE active = 1) "
    }
}
