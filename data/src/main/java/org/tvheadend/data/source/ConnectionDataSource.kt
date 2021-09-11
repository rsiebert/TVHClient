package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.Connection
import org.tvheadend.data.entity.ConnectionEntity
import org.tvheadend.data.entity.ServerStatus
import org.tvheadend.data.entity.ServerStatusEntity
import timber.log.Timber
import java.util.*

class ConnectionDataSource(private val db: AppRoomDatabase) : DataSourceInterface<Connection> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    val liveDataActiveItem: LiveData<Connection>
        get() = Transformations.map(db.connectionDao.loadActiveConnection()) { entity ->
            Timber.d("Active live data item for connection is null ${entity == null}")
            entity?.toConnection() ?: Connection()
        }

    val activeItem: Connection
        get() {
            var connection = Connection().also { it.id = -1 }
            runBlocking(Dispatchers.IO) {
                val c = db.connectionDao.loadActiveConnectionSync()
                if (c != null) {
                    Timber.d("Loaded active connection ${c.name} with id ${c.id}")
                    connection = c.toConnection()
                }
            }
            Timber.d("Returning active connection ${connection.name} with id ${connection.id}")
            return connection
        }

    override fun addItem(item: Connection) {
        ioScope.launch {
            if (item.isActive) {
                db.connectionDao.disableActiveConnection()
            }
            val newId = db.connectionDao.insert(ConnectionEntity.from(item))
            // Create a new server status row in the database
            // that is linked to the newly added connection
            val serverStatus = ServerStatus()
            serverStatus.connectionId = newId.toInt()
            db.serverStatusDao.insert(ServerStatusEntity.from(serverStatus))
        }
    }

    override fun updateItem(item: Connection) {
        ioScope.launch {
            if (item.isActive) {
                db.connectionDao.disableActiveConnection()
            }
            db.connectionDao.update(ConnectionEntity.from(item))
        }
    }

    override fun removeItem(item: Connection) {
        ioScope.launch {
            db.connectionDao.delete(ConnectionEntity.from(item))
            db.serverStatusDao.deleteByConnectionId(item.id)
        }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return db.connectionDao.connectionCount
    }

    override fun getLiveDataItems(): LiveData<List<Connection>> {
        return Transformations.map(db.connectionDao.loadAllConnections()) { entities ->
            entities.map { it.toConnection() }
        }
    }

    override fun getLiveDataItemById(id: Any): LiveData<Connection> {
        return Transformations.map(db.connectionDao.loadConnectionById(id as Int)) { entity ->
            entity.toConnection()
        }
    }

    override fun getItemById(id: Any): Connection? {
        var connection: Connection?
        runBlocking(Dispatchers.IO) {
            connection = db.connectionDao.loadConnectionByIdSync(id as Int)?.toConnection()
        }
        return connection
    }

    override fun getItems(): List<Connection> {
        val connections = ArrayList<Connection>()
        runBlocking(Dispatchers.IO) {
            connections.addAll(db.connectionDao.loadAllConnectionsSync().map { it.toConnection() })
        }
        return connections
    }

    fun setSyncRequiredForActiveConnection() {
        val connection = activeItem
        if (connection.id >= 0) {
            connection.isSyncRequired = true
            connection.lastUpdate = 0
            updateItem(connection)
        }
    }

    fun switchActiveConnection(oldId: Int, newId: Int) {
        Timber.d("Switching active connection from id $oldId to $newId")
        runBlocking(Dispatchers.IO) {
            db.connectionDao.loadConnectionByIdSync(oldId)?.also {
                Timber.d("Currently active connection is ${it.name} with id ${it.id}")
                it.isActive = false
                db.connectionDao.update(it)
            }
            db.connectionDao.loadConnectionByIdSync(newId)?.also {
                Timber.d("New active connection shall be ${it.name} with id ${it.id}")
                it.isActive = true
                it.isSyncRequired = true
                it.lastUpdate = 0
                db.connectionDao.update(it)
            }
            db.connectionDao.loadActiveConnectionSync()?.let {
                Timber.d("New active connection is be ${it.name} with id ${it.id}")
            }
        }
    }
}
