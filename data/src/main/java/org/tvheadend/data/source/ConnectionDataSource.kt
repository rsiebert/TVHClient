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
import org.tvheadend.data.entity.Connection
import org.tvheadend.data.entity.ConnectionEntity
import org.tvheadend.data.entity.ServerStatus
import org.tvheadend.data.entity.ServerStatusEntity
import timber.log.Timber
import java.util.*

class ConnectionDataSource(
        private val db: AppRoomDatabase,
        private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : DataSourceInterface<Connection> {

    // See ChannelDataSource: reads follow the injected scope's dispatcher (Dispatchers.IO in
    // production, Unconfined in the unit tests).
    private val ioDispatcher: CoroutineDispatcher =
            ioScope.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher ?: Dispatchers.IO

    val liveDataActiveItem: LiveData<Connection>
        get() = db.connectionDao.loadActiveConnection().map { entity ->
            Timber.d("Active live data item for connection is null ${entity == null}")
            // Fall back to the same "no active connection" sentinel as the synchronous
            // activeItem (id = -1). A plain Connection() has id 0, which callers that test
            // `id >= 0` - setSyncRequiredForActiveConnection, for one - misread as a real
            // connection.
            entity?.toConnection() ?: Connection().also { it.id = -1 }
        }

    suspend fun getActiveItem(): Connection {
        var connection = Connection().also { it.id = -1 }
        withContext(ioDispatcher) {
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
        return db.connectionDao.loadAllConnections().map { entities ->
            entities.map { it.toConnection() }
        }
    }

    override suspend fun getItemById(id: Any): Connection? {
        var connection: Connection?
        withContext(ioDispatcher) {
            connection = db.connectionDao.loadConnectionByIdSync(id as Int)?.toConnection()
        }
        return connection
    }

    override suspend fun getItems(): List<Connection> {
        val connections = ArrayList<Connection>()
        withContext(ioDispatcher) {
            connections.addAll(db.connectionDao.loadAllConnectionsSync().map { it.toConnection() })
        }
        return connections
    }

    suspend fun setSyncRequiredForActiveConnection() {
        val connection = getActiveItem()
        if (connection.id >= 0) {
            connection.isSyncRequired = true
            connection.lastUpdate = 0
            updateItem(connection)
        }
    }

    suspend fun switchActiveConnection(oldId: Int, newId: Int) {
        Timber.d("Switching active connection from id $oldId to $newId")
        withContext(ioDispatcher) {
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
