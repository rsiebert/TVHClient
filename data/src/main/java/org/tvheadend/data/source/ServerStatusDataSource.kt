package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.ServerStatus
import org.tvheadend.data.entity.ServerStatusEntity
import timber.log.Timber
import java.util.*

class ServerStatusDataSource(
        private val db: AppRoomDatabase,
        private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : DataSourceInterface<ServerStatus> {

    // See ChannelDataSource: reads follow the injected scope's dispatcher (Dispatchers.IO in
    // production, Unconfined in the unit tests).
    private val ioDispatcher: CoroutineDispatcher =
            ioScope.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher ?: Dispatchers.IO

    val liveDataActiveItem: LiveData<ServerStatus>
        get() = db.serverStatusDao.loadActiveServerStatus().map { entity ->
            Timber.d("Loading active server status as live data is null ${entity == null}")
            // When there is no active server status row, fall back to the same in-memory
            // "unknown" sentinel that the suspend getActiveItem() returns without a connection.
            // This map runs on the LiveData observer's thread, so it must stay non-blocking.
            entity?.toServerStatus() ?: ServerStatus().also {
                it.serverName = "Unknown"
                it.serverVersion = "Unknown"
            }
        }

    suspend fun getActiveItem(): ServerStatus {
        var serverStatus = ServerStatus()
        withContext(ioDispatcher) {
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
        return db.serverStatusDao.loadAllServerStatus().map { entities ->
            entities.map { it.toServerStatus() }
        }
    }

    override suspend fun getItemById(id: Any): ServerStatus? {
        var serverStatus: ServerStatus?
        withContext(ioDispatcher) {
            serverStatus = db.serverStatusDao.loadServerStatusByIdSync(id as Int)?.toServerStatus()
        }
        return serverStatus
    }

    override suspend fun getItems(): List<ServerStatus> {
        return ArrayList()
    }
}
