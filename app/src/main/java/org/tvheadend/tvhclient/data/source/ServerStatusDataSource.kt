package org.tvheadend.tvhclient.data.source

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import timber.log.Timber
import java.util.*

class ServerStatusDataSource(private val db: AppRoomDatabase) : DataSourceInterface<ServerStatus> {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    val liveDataActiveItem: LiveData<ServerStatus>
        get() = db.serverStatusDao.loadActiveServerStatus()

    val activeItem: ServerStatus
        get() {
            var serverStatus = ServerStatus()
            runBlocking(Dispatchers.IO) {
                val newServerStatus = db.serverStatusDao.loadActiveServerStatusSync()
                if (newServerStatus == null) {
                    Timber.d("Active server status is null, trying to add new one with default values")
                    val connection = db.connectionDao.loadActiveConnectionSync()
                    serverStatus.serverName = "Unknown"
                    serverStatus.serverVersion = "Unknown"
                    if (connection != null) {
                        serverStatus.connectionId = connection.id
                        serverStatus.connectionName = connection.name
                    }
                } else {
                    serverStatus = newServerStatus
                }
            }
            return serverStatus
        }

    override fun addItem(item: ServerStatus) {
        ioScope.launch { db.serverStatusDao.insert(item) }
    }

    override fun updateItem(item: ServerStatus) {
        ioScope.launch { db.serverStatusDao.update(item) }
    }

    override fun removeItem(item: ServerStatus) {
        ioScope.launch { db.serverStatusDao.delete(item) }
    }

    override fun getLiveDataItemCount(): LiveData<Int> {
        return db.serverStatusDao.serverStatusCount
    }

    override fun getLiveDataItems(): LiveData<List<ServerStatus>> {
        return db.serverStatusDao.loadAllServerStatus()
    }

    override fun getLiveDataItemById(id: Any): LiveData<ServerStatus> {
        return db.serverStatusDao.loadServerStatusById(id as Int)
    }

    override fun getItemById(id: Any): ServerStatus? {
        var serverStatus: ServerStatus? = null
        runBlocking(Dispatchers.IO) {
            serverStatus = db.serverStatusDao.loadServerStatusByIdSync(id as Int)
        }
        return serverStatus
    }

    override fun getItems(): List<ServerStatus> {
        return ArrayList()
    }
}
