package org.tvheadend.tvhclient.domain.repository.data_source

import android.os.AsyncTask
import androidx.lifecycle.LiveData
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import timber.log.Timber
import java.util.*
import java.util.concurrent.ExecutionException

class ServerStatusData(private val db: AppRoomDatabase) : DataSourceInterface<ServerStatus> {

    val liveDataActiveItem: LiveData<ServerStatus>
        get() = db.serverStatusDao.loadActiveServerStatus()

    val activeItem: ServerStatus
        get() {
            try {
                return ActiveServerStatusTask(db).execute().get()
            } catch (e: InterruptedException) {
                Timber.d(e, "Loading active server status task got interrupted")
            } catch (e: ExecutionException) {
                Timber.d(e, "Loading active server status task aborted")
            }
            // Create a new server status object with the connection id
            val serverStatus = ServerStatus()
            serverStatus.connectionId = db.connectionDao.loadActiveConnectionSync().id
            addItem(serverStatus)
            return serverStatus
        }

    override fun addItem(item: ServerStatus) {
        AsyncTask.execute { db.serverStatusDao.insert(item) }
    }

    override fun updateItem(item: ServerStatus) {
        AsyncTask.execute { db.serverStatusDao.update(item) }
    }

    override fun removeItem(item: ServerStatus) {
        AsyncTask.execute { db.serverStatusDao.delete(item) }
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
        try {
            return ServerStatusByIdTask(db, id as Int).execute().get()
        } catch (e: InterruptedException) {
            Timber.d(e, "Loading server status by id task got interrupted")
        } catch (e: ExecutionException) {
            Timber.d(e, "Loading server status by id task aborted")
        }
        return null
    }

    override fun getItems(): List<ServerStatus> {
        return ArrayList()
    }

    private class ServerStatusByIdTask internal constructor(private val db: AppRoomDatabase, private val id: Int) : AsyncTask<Void, Void, ServerStatus>() {

        override fun doInBackground(vararg voids: Void): ServerStatus {
            return db.serverStatusDao.loadServerStatusByIdSync(id)
        }
    }

    private class ActiveServerStatusTask internal constructor(private val db: AppRoomDatabase) : AsyncTask<Void, Void, ServerStatus>() {

        override fun doInBackground(vararg voids: Void): ServerStatus {
            return db.serverStatusDao.loadActiveServerStatusSync()
        }
    }
}
