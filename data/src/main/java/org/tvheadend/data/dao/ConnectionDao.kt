package org.tvheadend.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.tvheadend.data.entity.ConnectionEntity

@Dao
internal interface ConnectionDao {

    @get:Query("SELECT COUNT (*) FROM connections")
    val connectionCount: LiveData<Int>

    @Query("SELECT * FROM connections")
    fun loadAllConnections(): LiveData<List<ConnectionEntity>>

    @Query("SELECT * FROM connections")
    fun loadAllConnectionsSync(): List<ConnectionEntity>

    @Query("SELECT * FROM connections WHERE active = 1")
    fun loadActiveConnection(): LiveData<ConnectionEntity>

    @Query("SELECT * FROM connections WHERE active = 1")
    fun loadActiveConnectionSync(): ConnectionEntity?

    @Query("SELECT * FROM connections WHERE id = :id")
    fun loadConnectionByIdSync(id: Int): ConnectionEntity

    @Query("SELECT * FROM connections WHERE id = :id")
    fun loadConnectionById(id: Int): LiveData<ConnectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(connection: ConnectionEntity): Long

    @Update
    fun update(vararg connection: ConnectionEntity)

    @Delete
    fun delete(connection: ConnectionEntity)

    @Query("UPDATE connections SET active = 0 WHERE active = 1")
    fun disableActiveConnection()
}
