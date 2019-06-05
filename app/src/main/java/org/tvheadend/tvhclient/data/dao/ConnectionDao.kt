package org.tvheadend.tvhclient.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.tvheadend.tvhclient.domain.entity.Connection

@Dao
interface ConnectionDao {

    @get:Query("SELECT COUNT (*) FROM connections")
    val connectionCount: LiveData<Int>

    @Query("SELECT * FROM connections")
    fun loadAllConnections(): LiveData<List<Connection>>

    @Query("SELECT * FROM connections")
    fun loadAllConnectionsSync(): List<Connection>

    @Query("SELECT * FROM connections WHERE active = 1")
    fun loadActiveConnection(): LiveData<Connection>

    @Query("SELECT * FROM connections WHERE active = 1")
    fun loadActiveConnectionSync(): Connection

    @Query("SELECT * FROM connections WHERE id = :id")
    fun loadConnectionByIdSync(id: Int): Connection

    @Query("SELECT * FROM connections WHERE id = :id")
    fun loadConnectionById(id: Int): LiveData<Connection>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(connection: Connection): Long

    @Update
    fun update(vararg connection: Connection)

    @Delete
    fun delete(connection: Connection)

    @Query("UPDATE connections SET active = 0 WHERE active = 1")
    fun disableActiveConnection()
}
