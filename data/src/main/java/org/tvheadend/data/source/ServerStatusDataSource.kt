package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.ServerStatus
import org.tvheadend.data.entity.ServerStatusEntity
import timber.log.Timber
import java.util.*

class ServerStatusDataSource(private val db: AppRoomDatabase) : DataSourceInterface<ServerStatus> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    val liveDataActiveItem: LiveData<ServerStatus>
        get() = Transformations.map(db.serverStatusDao.loadActiveServerStatus()) { entity ->
            Timber.d("Loading active server status as live data is null ${entity == null}")
            entity?.toServerStatus() ?: activeItem
        }

    val activeItem: ServerStatus
        get() {
            var serverStatus = ServerStatus()
            runBlocking(Dispatchers.IO) {
                val newServerStatus = db.serverStatusDao.loadActiveServerStatusSync()
                if (newServerStatus == null) {
                    Timber.d("Active server status is null")
                    val connection = db.connectionDao.loadActiveConnectionSync()
                    serverStatus.serverName = "Unknown"
                    serverStatus.serverVersion = "Unknown"
                    if (connection != null) {
                        Timber.d("Loaded active connection for empty server status")
                        serverStatus.connectionId = connection.id
                        serverStatus.connectionName = connection.name
                        Timber.d("Inserting new server status information for connection ${connection.name}")
                        db.serverStatusDao.insert(ServerStatusEntity.from(serverStatus))
                    }
                } else {
                    serverStatus = newServerStatus.toServerStatus()
                }
            }
            return serverStatus
        }

    override fun addItem(item: ServerStatus) {
        ioScope.launch { db.serverStatusDao.insert(ServerStatusEntity.from(item)) }
    }

    override fun updateItem(item: ServerStatus) {
        ioScope.launch { db.serverStatusDao.update(ServerStatusEntity.from(item)) }
    }

    override fun removeItem(item: ServerStatus) {
        ioScope.launch { db.serverStatusDao.delete(ServerStatusEntity.from(item)) }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return db.serverStatusDao.serverStatusCount
    }

    override fun getLiveDataItems(): LiveData<List<ServerStatus>> {
        return Transformations.map(db.serverStatusDao.loadAllServerStatus()) { entities ->
            entities.map { it.toServerStatus() }
        }
    }

    override fun getLiveDataItemById(id: Any): LiveData<ServerStatus> {
        return Transformations.map(db.serverStatusDao.loadServerStatusById(id as Int)) { entity ->
            entity.toServerStatus()
        }
    }

    override fun getItemById(id: Any): ServerStatus? {
        var serverStatus: ServerStatus?
        runBlocking(Dispatchers.IO) {
            serverStatus = db.serverStatusDao.loadServerStatusByIdSync(id as Int)?.toServerStatus()
        }
        return serverStatus
    }

    override fun getItems(): List<ServerStatus> {
        return ArrayList()
    }
}
