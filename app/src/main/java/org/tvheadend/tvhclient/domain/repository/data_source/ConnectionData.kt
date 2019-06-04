package org.tvheadend.tvhclient.domain.repository.data_source

import android.os.AsyncTask
import androidx.lifecycle.LiveData
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import timber.log.Timber
import java.util.*
import java.util.concurrent.ExecutionException

class ConnectionData(private val db: AppRoomDatabase) : DataSourceInterface<Connection> {

    val activeItem: Connection
        get() {
            try {
                return ConnectionByIdTask(db).execute().get() ?: Connection().also { it.id = -1 }
            } catch (e: InterruptedException) {
                Timber.e(e, "Loading active connection task got interrupted")
            } catch (e: ExecutionException) {
                Timber.e(e, "Loading active connection task aborted")
            }
            return Connection().also { it.id = -1 }
        }

    override fun addItem(item: Connection) {
        AsyncTask.execute {
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
        AsyncTask.execute {
            if (item.isActive) {
                db.connectionDao.disableActiveConnection()
            }
            db.connectionDao.update(item)
        }
    }

    override fun removeItem(item: Connection) {
        AsyncTask.execute {
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
        try {
            return ConnectionByIdTask(db, id as Int).execute().get()
        } catch (e: InterruptedException) {
            Timber.e(e, "Loading connection by id task got interrupted")
        } catch (e: ExecutionException) {
            Timber.e(e, "Loading connection by id task aborted")
        }

        return null
    }

    override fun getItems(): List<Connection> {
        val connections = ArrayList<Connection>()
        try {
            connections.addAll(ConnectionListTask(db).execute().get())
        } catch (e: InterruptedException) {
            Timber.e(e, "Loading all connections task got interrupted")
        } catch (e: ExecutionException) {
            Timber.e(e, "Loading all connections task aborted")
        }

        return connections
    }

    internal class ConnectionByIdTask : AsyncTask<Void, Void, Connection?> {
        private val db: AppRoomDatabase
        private val id: Int

        constructor(db: AppRoomDatabase, id: Int) {
            this.db = db
            this.id = id
        }

        constructor(db: AppRoomDatabase) {
            this.db = db
            this.id = -1
        }

        override fun doInBackground(vararg voids: Void): Connection {
            return if (id < 0) {
                db.connectionDao.loadActiveConnectionSync()
            } else {
                db.connectionDao.loadConnectionByIdSync(id)
            }
        }
    }

    private class ConnectionListTask internal constructor(private val db: AppRoomDatabase) : AsyncTask<Void, Void, List<Connection>>() {

        override fun doInBackground(vararg voids: Void): List<Connection> {
            return db.connectionDao.loadAllConnectionsSync()
        }
    }
}
