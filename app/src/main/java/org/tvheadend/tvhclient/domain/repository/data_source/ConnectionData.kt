package org.tvheadend.tvhclient.domain.repository.data_source

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import java.util.*

class ConnectionData(private val db: AppRoomDatabase) : DataSourceInterface<Connection> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    val activeItem: Connection
        get() {
            var connection = Connection().also { it.id = -1 }
            runBlocking(Dispatchers.IO) {
                val c = db.connectionDao.loadActiveConnectionSync()
                if (c != null) {
                    connection = c
                }
            }
            return connection
        }

    override fun addItem(item: Connection) {
        ioScope.launch {
            if (item.isActive) {
                db.connectionDao.disableActiveConnection()
            }
            val newId = db.connectionDao.insert(item)
            // Create a new server status row in the database
            // that is linked to the newly added connection
            val serverStatus = ServerStatus()
            serverStatus.connectionId = newId.toInt()
            db.serverStatusDao.insert(serverStatus)
        }
    }

    override fun updateItem(item: Connection) {
        ioScope.launch {
            if (item.isActive) {
                db.connectionDao.disableActiveConnection()
            }
            db.connectionDao.update(item)
        }
    }

    override fun removeItem(item: Connection) {
        ioScope.launch {
            db.connectionDao.delete(item)
            db.serverStatusDao.deleteByConnectionId(item.id)
        }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return db.connectionDao.connectionCount
    }

    override fun getLiveDataItems(): LiveData<List<Connection>> {
        return db.connectionDao.loadAllConnections()
    }

    override fun getLiveDataItemById(id: Any): LiveData<Connection> {
        return db.connectionDao.loadConnectionById(id as Int)
    }

    override fun getItemById(id: Any): Connection? {
        var connection: Connection? = null
        runBlocking(Dispatchers.IO) {
            connection = db.connectionDao.loadConnectionByIdSync(id as Int)
        }
        return connection
    }

    override fun getItems(): List<Connection> {
        val connections = ArrayList<Connection>()
        runBlocking(Dispatchers.IO) {
            connections.addAll(db.connectionDao.loadAllConnectionsSync())
        }
        return connections
    }
}
