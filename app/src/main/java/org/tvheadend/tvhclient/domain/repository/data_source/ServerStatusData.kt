package org.tvheadend.tvhclient.domain.repository.data_source

import android.os.AsyncTask
import androidx.lifecycle.LiveData
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import timber.log.Timber
import java.util.*
import java.util.concurrent.ExecutionException

class ServerStatusData(private val db: AppRoomDatabase) : DataSourceInterface<ServerStatus> {

    val liveDataActiveItem: LiveData<ServerStatus>?
        get() = db.serverStatusDao.loadActiveServerStatus()

    val activeItem: ServerStatus?
        get() {
            try {
                return ActiveServerStatusTask(db).execute().get()
            } catch (e: InterruptedException) {
                Timber.d("Loading active server status task got interrupted", e)
            } catch (e: ExecutionException) {
                Timber.d("Loading active server status task aborted", e)
            }
            return null
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

    override fun getLiveDataItemCount(): LiveData<Int>? {
        return null
    }

    override fun getLiveDataItems(): LiveData<List<ServerStatus>>? {
        return null
    }

    override fun getLiveDataItemById(id: Any): LiveData<ServerStatus>? {
        return db.serverStatusDao.loadServerStatusById(id as Int)
    }

    override fun getItemById(id: Any): ServerStatus? {
        try {
            return ServerStatusByIdTask(db, id as Int).execute().get()
        } catch (e: InterruptedException) {
            Timber.d("Loading server status by id task got interrupted", e)
        } catch (e: ExecutionException) {
            Timber.d("Loading server status by id task aborted", e)
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

        override fun doInBackground(vararg voids: Void): ServerStatus? {

            var serverStatus: ServerStatus? = db.serverStatusDao.loadActiveServerStatusSync()
            if (serverStatus == null) {
                var msg = "Trying to get active server status from database returned no entry."
                Timber.e(msg)
                if (Fabric.isInitialized()) {
                    Crashlytics.logException(Exception(msg))
                }

                val connection = db.connectionDao.loadActiveConnectionSync()
                serverStatus = ServerStatus().also { it.connectionId = connection.id }
                db.serverStatusDao.insert(serverStatus)

                msg = "Trying to get active server status from database returned no entry.\n" +
                        "Inserted new server status for active connection " + connection.id
                Timber.e(msg)
                if (Fabric.isInitialized()) {
                    Crashlytics.logException(Exception(msg))
                }
            }
            return serverStatus
        }
    }
}
